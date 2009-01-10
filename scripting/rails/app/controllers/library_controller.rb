class LibraryController < ApplicationController
  def index
    @files = Limewire::Library.all_files
  end

  def show
    file = Limewire::Library.find("urn:sha1:#{params[:sha1]}")
    if file
      path = file.get_file.absolute_path
      
      if File.exist?(path)
        response.headers["Accepted-Ranges"] = "bytes 0-#{file.get_file_size}"
        if request.env["HTTP_RANGE"] # && request.env["HTTP_RANGE"] == "bytes=0-1"
          puts "getting file in chunks"
          puts request.headers.to_yaml
          range = request.env["HTTP_RANGE"].split("=").last
          puts range
          start, finish = request.env["HTTP_RANGE"].split("-").map{|num| num.to_i}
          
          size = finish - start + 1
          File.open(path) do |f|
            f.seek(start)
            @data = f.read(size)
          end
          puts @data if finish > 0
          response.headers["Content-Range"] = "bytes #{start}-#{finish}/#{size}"
          response.headers["Content-Length"] = "#{size}"
          send_data(@data, :status => 206, :type => "audio/mpeg")
        else
          send_file(path)
        end
      else
        render :text => "file not found: #{path}", :status => 404
      end
    else
      render :status => 404, :text => "file not found: #{params[:sha1]}"
    end
  end
end
