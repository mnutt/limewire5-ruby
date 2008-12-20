search_for = File.join("core","com","limegroup","scripting","resources")
$LOAD_PATH.unshift(File.join($LOAD_PATH.find{|t| t.scan(search_for).first} || '.', "lib"))

require 'francis/francis.rb'
require 'erb'

require 'htmlentities/lib/htmlentities.rb'
require 'limewire.rb'
require 'json.rb'

import 'org.apache.http.nio.entity.NStringEntity'
import 'org.apache.http.nio.entity.NFileEntity'


#I wonder if there is someway we could parse this handler only once,
#keep it in memory in javaland, and then just call dispatch() 
#when we need to 

handler = Francis.new do
  setup do
    Limewire.core = $core
  end

  dispatcher do
    #apache java http request object extracting
    @uri = @request.request_line.uri.split("?",2).first
    @query_string = @request.request_line.uri.split("?",2)[1].split("&").inject({}) {|memo,pair|
     ls,rs = pair.split("=",2) 
      memo[ls] = rs
      memo
    } rescue nil
#    @headers = @request.get_headers
#    @params = @request.get_params
  end

  responder do
    if @response.json
      @response.body = @response.json.to_json
      @response.content_type = 'text/x-json'
    end
     
    if @response.body
      nstring = NStringEntity.new(@response.body)

      content_type = if @response.content_type != "auto"
                       @response.content_type
                     else
                       "text/html"
                     end

      nstring.setContentType(content_type)
      nstring
    else
      contents = if @response.file_name
                   java.io.File.new("core/com/limegroup/scripting/resources/assets/" + @response.file_name)
                 elsif @response.file
                   @response.file
                 else
                   "ERROR! no file or body specified"
                 end

      content_type = if @response.content_type != "auto"
                       @response.content_type
                     else
                       extn = @response.file_name.split(".").last

                       case extn
                       when "html"
                         "text/html"
                       when "html"
                         "text/html"
                       when "css"
                         "text/css"
                       when "js"
                         "text/javascript"
                       when "txt"
                         "text/plain"
                       when "swf"
                         "application/x-shockwave-flash"
                       else
                         "application/binary"
                       end
                     end

        file_entity = NFileEntity.new(contents, content_type, false)
        file_entity.setContentType(content_type)
        file_entity

    end
  end

  get '/script' do
    response.body = "Welcome to Limewire!"
  end

  get '/script/playlist.xspf' do
    @files = Limewire::Library.all_files.select{|f| f.file_name =~ /\.mp3$/}
    response.body = erb "xspf.erb"
  end

  get '/script/playlist' do
    @files = Limewire::Library.all_files.select{|f| f.file_name =~ /\.mp3$/}
    response.body = erb "playlist.erb"
  end

  get '/script/search_results' do
    @q = request.query_string["q"]
    @guid = request.query_string["guid"]
    puts "Handling: #{@guid}, and #{@q}"
    if @guid
      @search = Limewire::Search.find(@guid)
      response.json = {"guid"=>@guid, 
                       "query"=>@search.query_string, 
                       "results"=> @search.results }
    elsif @q
      @q.gsub!(/%20/, " ")
      @search = Limewire::Search.query(@q)
      @search.start
      response.json = {"guid"=> @search.guid}
    end

  end

  get '/script/search' do
    response.body = erb "search.erb"
  end

  get %r{/script/sc/tracks.json} do
    #Limewire::Library.filter_by_name(/mp3$/)[0..1].collect(&:to_cloud).to_json
    response.json = Limewire::Library.filter_by_name(/mp3$/).collect{|x| x.to_cloud}
#    puts response.json
  end
  get '/script/e' do
    #eval(request.query_string["ev"])
    val = eval(java.net.URLDecoder.decode(request.query_string["ev"]))
    (ret ||=[]) << "To_s: " + val.to_s
    ret << "each to_s:" + val.collect{|x| x.to_s}.join("<br>") rescue "none"
    ret << "json:" + JSON(val) rescue "none"
    ret << "Class:<b>#{val.class.to_s.tr("<>", "()")}</b>"
    ret << "Size:<b>#{val.size rescue "n/a"}</b>"
    ret << "Methods:<br>" + val.methods.sort.collect{|x| ((x=~/^get/) ? "<b>#{x}</b>" : x) }.join("<br>")
    response.body = ret.join ("<br>")
  end
  
  get '/script/stats' do
    puts @request.query_string
    @uptime = Limewire.uptime / 1000
    @daily_uptime = Limewire.daily_uptime

    @files = Limewire::Library.all_files
    @categories = Limewire::Library.categories
    response.body = erb 'stats.erb'
  end

end

handler.dispatch($request)
