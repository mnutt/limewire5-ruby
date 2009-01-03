class LibraryController < ApplicationController
  def index
    @files = Limewire::Library.all_files
  end

  def show
    file = Limewire::Library.find(params[:sha1])
    path = file.get_file.absolute_path

    if File.exist?(path)
      send_file(path, :disposition => 'inline')
    else
      render :text => "file not found: #{path}", :status => 404
    end
  end
end
