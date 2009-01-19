class BackupController < PluginController
  def index
    @library_size = Limewire::Library.find(:all).inject(0) { |r, f| r + f.file_size }
    @transfer_rate = 0.10 / 1.gigabyte
    @storage_rate = 0.15 / 1.gigabyte
  end

  def create
    my_guid = Limewire.my_guid.to_s

    @s3 = RightAws::S3.new(params[:aws_key], params[:aws_secret], {:port => '80', :protocol => 'http'})
    @bucket = @s3.bucket("limewire-backup", true)
    
    @files = Limewire::Library.find(:all)
    PersistentStore[:transfer_library_size] = @files.inject(0) { |r, f| r + f.file_size }
    PersistentStore[:transfer_total_bytes_written] = 0 # reset total written

    PersistentStore[:transfer_thread] = Thread.new do
      @files.each_with_index do |file, i|
        begin
          PersistentStore[:transfer_current_file] = file.file_name.to_s
          PersistentStore[:transfer_current_file_size] = file.file_size
          PersistentStore[:transfer_current_index] = i + 1
          PersistentStore[:transfer_file_bytes_written] = 0 # reset during each file
          
          path = "#{my_guid}--#{file.sha1}--#{file.get_file.absolute_path}"
          
          puts "Sending key: #{path}"
          @bucket.put(path, File.open(file.get_file.absolute_path, "rb"))
          
          PersistentStore[:transfer_total_bytes_written] += file.file_size # update total 
        rescue
          PersistentStore[:transfer_library_size] -= file.file_size
          puts $!.message
        end      
      end
    end

    render :json => {:state => PersistentStore[:transfer_thread].status}
  end

  def status
    info = { :library_size => PersistentStore[:transfer_library_size],
             :total_bytes_written => PersistentStore[:transfer_total_bytes_written] + PersistentStore[:transfer_file_bytes_written],
             :file_bytes_written => PersistentStore[:transfer_file_bytes_written],
             :current_index => PersistentStore[:transfer_current_index],
             :total_files => Limewire::Library.count,
             :current_file_size => PersistentStore[:transfer_current_file_size],
             :current_file => PersistentStore[:transfer_current_file],
             :state => PersistentStore[:transfer_thread].status }
    render :json => info
  end

  def stop
    thread = PersistentStore[:transfer_thread]
    thread.exit rescue nil
    render :json => {:state => thread.status}
  end

  def widget
  end
end
