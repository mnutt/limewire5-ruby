#I wanted to call it SinatraJr, but this is more fitting
#get '/index.html'(string) matches exactly
#get /foo/ matches regex

$LOAD_PATH << File.expand_path(File.dirname(__FILE__) + "/../..")

require 'erb'
require 'lib/htmlentities/lib/htmlentities.rb'

class Francis
  class RoutingError < Exception; end


  #Session embodies a page load request/response
  #within this context all 'get' blocks are executed
  class Session
    attr_reader :response
    attr_reader :request

    #embodies any response data
    # #json overrides #body, #body overrides #file_name, which overrides #file
    #content_type defaults to auto, which it will try to guess based on the file_name ext
    #auto does not work if you set #file=, must set it manually
    #content_type "auto" for #body defaults to text/html
    class ResponseData
      attr_accessor :content_type
      attr_accessor :body
      attr_accessor :file
      attr_accessor :file_name
      attr_accessor :json

      def initialize(opts)
        @content_type = opts[:content_type]
        @body = opts[:body] || nil
        @file = @file_name = nil
        @json = nil
      end
    end

    def initialize(opts)
      opts = opts.merge({ :content_type => "auto",
                          :data => opts[:data] || {}, #wheeee
                        })

      @response = ResponseData.new(opts)

      @request = opts[:request]

      #tack on any extra data which is passed into dispatch
      def @request.user_data=(d); @user_data = d; end
      def @request.user_data; @user_data; end
      @request.user_data = opts[:data]
      def @request.query_string=(d); @query_string = d; end
      def @request.query_string; @query_string; end
      @request.query_string = opts[:query_string] || {}

    end
    
    #parse erb templates
    def erb(o, b=binding)
      template = case o
                 when File
                   open(o).read
                 when String
                   if o[-4..-1] == ".erb"
                     f = open("../../core/com/limegroup/scripting/resources/templates/" + o)
                     ret = f.read
                     f.close
                     ret
                   else
                     o
                   end
                 end

      ERB.new(template).result(b)
    end
  end

  def initialize(&b)
    @paths = Hash.new
    self.instance_eval(&b) if block_given?

    #execute setup block if it exists
    self.instance_eval(&@setup) if @setup
  end

  def setup(&b)
    @setup = b
  end

  #helper to extract common elements from request
  #request will still be available in unedited format
  def dispatcher(&b)
    @dispatcher = b
  end


  #this block should create the response object after processing is done
  def responder(&b)
    @responder = b 
  end

  def get(route, &block)
    #do any preprocessing/route expansion here
    @paths[route] = block
  end

  def dispatch(request, *data)
    @headers = @uri = @params = nil
    @request = request

    self.instance_eval(&@dispatcher) if @dispatcher

    data = data.first || {}
    
    b = @paths.find{|route, block| 
      case route
      when String
        @uri == route
      when Regexp
        @uri =~ route
      end
    }
    
    raise RoutingError.new("No known URI: " + @uri.to_s) if b == nil

    data[:match] = $~[1..-1] unless $~.nil?

    sess = Session.new(:uri => @uri || nil,
                       :headers => @headers || nil,
                       :params => @params || nil,
                       :query_string => @query_string || nil,
                       :request => request, 
                       :data => data
                       )

    sess.instance_eval(&b.last)
    @response = sess.response
    if @responder
      self.instance_eval(&@responder)
    else
      sess.response
    end
  end
end
