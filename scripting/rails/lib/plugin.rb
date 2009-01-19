class Plugin
  def self.all
    Dir.glob("#{PLUGIN_ROOT}/*").select {|f| File.directory?(f) }.map{|d| self.new(d) }
  end
  
  attr_reader :name
  attr_reader :path

  def initialize(path)
    @path = path
    @name = @path.split("/").last
  end

  # TODO: make this better, since they can remap the widget route
  def has_widget?
    File.exist?("#{@path}/views/#{@name}/widget.html.erb") || File.exist?("#{@path}/views/widget.erb")
  end
end
