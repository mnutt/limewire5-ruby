class LibraryController < ApplicationController
  def index
    @files = Limewire::Library.all_files
  end

  def show
    file = Limewire::Library.find("urn:sha1:#{params[:sha1]}")
    if file
      path = file.get_file.absolute_path
      
      if File.exist?(path)
        # Send file in chunks if iPhone is asking for it that way
        if request.env["HTTP_RANGE"]
          range = request.env["HTTP_RANGE"].split("=").last
          start, finish = request.env["HTTP_RANGE"].split("-").map{|num| num.to_i}
          size = finish - start + 1

          File.open(path) do |f|
            f.seek(start)
            @data = f.read(size)
          end

          response.headers["Accept-Ranges"]  = "bytes"
          response.headers["Content-Range"]  = "bytes #{start}-#{finish}/#{file.get_file_size}"
          response.headers["Content-Length"] = "#{size}"

          send_data(@data, :status => 206)
        else
          send_file(path)
        end
      else
        render :text => "record exists, but file not found: #{path}", :status => 404
      end
    else
      render :status => 404, :text => "file not found: #{params[:sha1]}"
    end
  end
end
