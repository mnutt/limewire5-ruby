class BackupController < ApplicationController
  def index
  end

  def create
    @s3 = RightAws::S3.new(params[:aws_key], params[:aws_secret])
    @bucket = @s3.bucket("limewire-backup", true)
    
    # @files = Limewire::Library.find(:all)
    # @files.each do |f|
    #   @bucket.put("#{f.sha1}--#{f.get_file.absolute_path}", File.open(f.get_file.absolute_path))
    # end
  end

  def show
  end
end
