class Library < Application

  def index
    render
  end

  def show
    @file = Limewire::Library.find(params[:sha1])
    send_file(@file.get_file)
  end
end
