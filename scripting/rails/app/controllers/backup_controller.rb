class BackupController < ApplicationController
  def index
  end

  def create
    @s3 = RightAws::S3.new(params[:aws_key], params[:aws_secret])
    @bucket = @s3.bucket("limewire-backup", true)
    raise @bucket.to_s
  end

  def show
  end
end
