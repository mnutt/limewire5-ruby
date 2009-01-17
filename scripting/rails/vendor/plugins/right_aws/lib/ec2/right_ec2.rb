#
# Copyright (c) 2007-2008 RightScale Inc
#
# Permission is hereby granted, free of charge, to any person obtaining
# a copy of this software and associated documentation files (the
# "Software"), to deal in the Software without restriction, including
# without limitation the rights to use, copy, modify, merge, publish,
# distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so, subject to
# the following conditions:
#
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
# LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
# OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
# WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#

module RightAws

  # = RightAWS::EC2 -- RightScale Amazon EC2 interface
  # The RightAws::EC2 class provides a complete interface to Amazon's
  # Elastic Compute Cloud service.
  # For explanations of the semantics
  # of each call, please refer to Amazon's documentation at
  # http://developer.amazonwebservices.com/connect/kbcategory.jspa?categoryID=87
  #
  # Examples:
  #
  # Create an EC2 interface handle:
  #   
  #   @ec2   = RightAws::Ec2.new(aws_access_key_id,
  #                               aws_secret_access_key)
  # Create a new SSH key pair:
  #  @key   = 'right_ec2_awesome_test_key'
  #  new_key = @ec2.create_key_pair(@key)
  #  keys = @ec2.describe_key_pairs
  #
  # Create a security group:
  #  @group = 'right_ec2_awesome_test_security_group'
  #  @ec2.create_security_group(@group,'My awesome test group')
  #  group = @ec2.describe_security_groups([@group])[0]
  #
  # Configure a security group:
  #  @ec2.authorize_security_group_named_ingress(@group, account_number, 'default')
  #  @ec2.authorize_security_group_IP_ingress(@group, 80,80,'udp','192.168.1.0/8')
  #
  # Describe the available images:
  #  images = @ec2.describe_images
  #
  # Launch an instance:
  #  ec2.run_instances('ami-9a9e7bf3', 1, 1, ['default'], @key, 'SomeImportantUserData', 'public')
  # 
  #
  # Describe running instances:
  #  @ec2.describe_instances
  #
  # Error handling: all operations raise an RightAws::AwsError in case
  # of problems. Note that transient errors are automatically retried.
    
  class Ec2 < RightAwsBase
    include RightAwsBaseInterface
    
    # Amazon EC2 API version being used
    API_VERSION       = "2008-02-01"
    DEFAULT_HOST      = "ec2.amazonaws.com"
    DEFAULT_PATH      = '/'
    DEFAULT_PROTOCOL  = 'https'
    DEFAULT_PORT      = 443
    
    # Default addressing type (public=NAT, direct=no-NAT) used when launching instances.
    DEFAULT_ADDRESSING_TYPE =  'public'
    DNS_ADDRESSING_SET      = ['public','direct']
    
    # Amazon EC2 Instance Types : http://www.amazon.com/b?ie=UTF8&node=370375011
    # Default EC2 instance type (platform) 
    DEFAULT_INSTANCE_TYPE   =  'm1.small' 
    INSTANCE_TYPES          = ['m1.small','c1.medium','m1.large','m1.xlarge','c1.xlarge']
    
    @@bench = AwsBenchmarkingBlock.new
    def self.bench_xml
      @@bench.xml
    end
    def self.bench_ec2
      @@bench.service
    end
    
     # Current API version (sometimes we have to check it outside the GEM).
    @@api = ENV['EC2_API_VERSION'] || API_VERSION
    def self.api 
      @@api
    end
    
    # Create a new handle to an EC2 account. All handles share the same per process or per thread
    # HTTP connection to Amazon EC2. Each handle is for a specific account. The params have the
    # following options:
    # * <tt>:server</tt>: EC2 service host, default: DEFAULT_HOST
    # * <tt>:port</tt>: EC2 service port, default: DEFAULT_PORT
    # * <tt>:protocol</tt>: 'http' or 'https', default: DEFAULT_PROTOCOL
    # * <tt>:multi_thread</tt>: true=HTTP connection per thread, false=per process
    # * <tt>:logger</tt>: for log messages, default: RAILS_DEFAULT_LOGGER else STDOUT
    # * <tt>:signature_version</tt>:  The signature version : '0' or '1'(default)
    #
    def initialize(aws_access_key_id=nil, aws_secret_access_key=nil, params={})
      init({ :name             => 'EC2', 
             :default_host     => ENV['EC2_URL'] ? URI.parse(ENV['EC2_URL']).host   : DEFAULT_HOST, 
             :default_port     => ENV['EC2_URL'] ? URI.parse(ENV['EC2_URL']).port   : DEFAULT_PORT,
             :default_service  => ENV['EC2_URL'] ? URI.parse(ENV['EC2_URL']).path   : DEFAULT_PATH,             
             :default_protocol => ENV['EC2_URL'] ? URI.parse(ENV['EC2_URL']).scheme : DEFAULT_PROTOCOL }, 
           aws_access_key_id    || ENV['AWS_ACCESS_KEY_ID'] , 
           aws_secret_access_key|| ENV['AWS_SECRET_ACCESS_KEY'],
           params)
    end


    def generate_request(action, params={}) #:nodoc:
      service_hash = {"Action"            => action,
                      "AWSAccessKeyId"    => @aws_access_key_id,
                      "Version"           => @@api,
                      "Timestamp"         => Time.now.utc.strftime("%Y-%m-%dT%H:%M:%S.000Z"),
                      "SignatureVersion"  => signature_version }
      service_hash.update(params)
      # prepare string to sight
      string_to_sign = case signature_version
                       when '0' then service_hash["Action"] + service_hash["Timestamp"]
                       when '1' then service_hash.sort{|a,b| (a[0].to_s.downcase)<=>(b[0].to_s.downcase)}.to_s
                       end
      service_hash.update('Signature' =>  AwsUtils::sign(@aws_secret_access_key, string_to_sign))
      request_params = service_hash.to_a.collect{|key,val| key + "=" + CGI::escape(val) }.join("&")
      request        = Net::HTTP::Get.new("#{@params[:service]}?#{request_params}")
        # prepare output hash
      { :request  => request, 
        :server   => @params[:server],
        :port     => @params[:port],
        :protocol => @params[:protocol] }
    end

      # Sends request to Amazon and parses the response
      # Raises AwsError if any banana happened
    def request_info(request, parser)  #:nodoc:
      thread = @params[:multi_thread] ? Thread.current : Thread.main
      thread[:ec2_connection] ||= Rightscale::HttpConnection.new(:exception => AwsError, :logger => @logger)
      request_info_impl(thread[:ec2_connection], @@bench, request, parser)
    end

    def request_cache_or_info(method, link, parser_class, use_cache=true) #:nodoc:
      # We do not want to break the logic of parsing hence will use a dummy parser to process all the standart 
      # steps (errors checking etc). The dummy parser does nothig - just returns back the params it received.
      # If the caching is enabled and hit then throw  AwsNoChange. 
      # P.S. caching works for the whole images list only! (when the list param is blank)      response, params = request_info(link, QEc2DummyParser.new)
      # check cache
      response, params = request_info(link, QEc2DummyParser.new)
      cache_hits?(method.to_sym, response.body) if use_cache
      parser = parser_class.new(:logger => @logger)
      @@bench.xml.add!{ parser.parse(response, params) }
      result = block_given? ? yield(parser) : parser.result
      # update parsed data
      update_cache(method.to_sym, :parsed => result) if use_cache
      result
    end

    def hash_params(prefix, list) #:nodoc:
      groups = {}
      list.each_index{|i| groups.update("#{prefix}.#{i+1}"=>list[i])} if list
      return groups
    end

  #-----------------------------------------------------------------
  #      Images
  #-----------------------------------------------------------------

    def ec2_describe_images(list, list_by='ImageId', image_type=nil) #:nodoc:
      request_hash = hash_params(list_by, list.to_a)
      request_hash['ImageType'] = image_type if image_type
      link = generate_request("DescribeImages", request_hash)
      request_cache_or_info :describe_images, link,  QEc2DescribeImagesParser, (list.blank? && list_by == 'ImageId' && image_type.blank?)
    rescue Exception
      on_exception
    end

      # Retrieve a list of images. Returns array of hashes describing the images or an exception:
      # +image_type+ = 'machine' || 'kernel' || 'ramdisk' 
      # 
      #  ec2.describe_images #=>
      #    [{:aws_owner => "522821470517",
      #      :aws_id => "ami-e4b6538d",
      #      :aws_state => "available",
      #      :aws_location => "marcins_cool_public_images/ubuntu-6.10.manifest.xml",
      #      :aws_is_public => true,
      #      :aws_architecture => "i386",
      #      :aws_image_type => "machine"},
      #     {...},
      #     {...} ]
      #
      # If +list+ param is set, then retrieve information about the listed images only:
      #
      #  ec2.describe_images(['ami-e4b6538d']) #=>
      #    [{:aws_owner => "522821470517",
      #      :aws_id => "ami-e4b6538d",
      #      :aws_state => "available",
      #      :aws_location => "marcins_cool_public_images/ubuntu-6.10.manifest.xml",
      #      :aws_is_public => true,
      #      :aws_architecture => "i386",
      #      :aws_image_type => "machine"}]
      #
    def describe_images(list=[], image_type=nil)
      ec2_describe_images(list, 'ImageId', image_type)
    end

      #
      #  Example:
      #
      #   ec2.describe_images_by_owner('522821470517')
      #   ec2.describe_images_by_owner('self')
      #
    def describe_images_by_owner(list, image_type=nil)
      ec2_describe_images(list, 'Owner', image_type)
    end

      #
      #  Example:
      #
      #   ec2.describe_images_by_executable_by('522821470517')
      #   ec2.describe_images_by_executable_by('self')
      #
    def describe_images_by_executable_by(list, image_type=nil)
      ec2_describe_images(list, 'ExecutableBy', image_type)
    end


      # Register new image at Amazon. 
      # Returns new image id or an exception.
      #
      #  ec2.register_image('bucket/key/manifest') #=> 'ami-e444444d'
      #
    def register_image(image_location)
      link = generate_request("RegisterImage", 
                              'ImageLocation' => image_location.to_s)
      request_info(link, QEc2RegisterImageParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end
    
      # Deregister image at Amazon. Returns +true+ or an exception.
      #
      #  ec2.deregister_image('ami-e444444d') #=> true
      #
    def deregister_image(image_id)
      link = generate_request("DeregisterImage", 
                              'ImageId' => image_id.to_s)
      request_info(link, RightBoolResponseParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end


      # Describe image attributes. Currently 'launchPermission', 'productCodes', 'kernel', 'ramdisk' and 'blockDeviceMapping'  are supported.
      #
      #  ec2.describe_image_attribute('ami-e444444d') #=> {:groups=>["all"], :users=>["000000000777"]}
      #
    def describe_image_attribute(image_id, attribute='launchPermission')
      link = generate_request("DescribeImageAttribute", 
                              'ImageId'   => image_id,
                              'Attribute' => attribute)
      request_info(link, QEc2DescribeImageAttributeParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end
    
      # Reset image attribute. Currently, only 'launchPermission' is supported. Returns +true+ or an exception.
      #
      #  ec2.reset_image_attribute('ami-e444444d') #=> true
      #
    def reset_image_attribute(image_id, attribute='launchPermission')
      link = generate_request("ResetImageAttribute", 
                              'ImageId'   => image_id,
                              'Attribute' => attribute)
      request_info(link, RightBoolResponseParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end

      # Modify an image's attributes. It is recommended that you use
      # modify_image_launch_perm_add_users, modify_image_launch_perm_remove_users, etc.
      # instead of modify_image_attribute because the signature of 
      # modify_image_attribute may change with EC2 service changes.
      #
      #  attribute      : currently, only 'launchPermission' is supported.
      #  operation_type : currently, only 'add' & 'remove' are supported.
      #  vars: 
      #    :user_group  : currently, only 'all' is supported.  
      #    :user_id
      #    :product_code
    def modify_image_attribute(image_id, attribute, operation_type = nil, vars = {})
      params =  {'ImageId'   => image_id,
                 'Attribute' => attribute}
      params['OperationType'] = operation_type if operation_type
      params.update(hash_params('UserId',      vars[:user_id].to_a))    if vars[:user_id]
      params.update(hash_params('UserGroup',   vars[:user_group].to_a)) if vars[:user_group]
      params.update(hash_params('ProductCode', vars[:product_code]))    if vars[:product_code]
      link = generate_request("ModifyImageAttribute", params)
      request_info(link, RightBoolResponseParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end

      # Grant image launch permissions to users.
      # Parameter +userId+ is a list of user AWS account ids.
      # Returns +true+ or an exception.
      #
      #  ec2.modify_image_launch_perm_add_users('ami-e444444d',['000000000777','000000000778']) #=> true
    def modify_image_launch_perm_add_users(image_id, user_id=[])
      modify_image_attribute(image_id, 'launchPermission', 'add', :user_id => user_id.to_a)
    end

      # Revokes image launch permissions for users. +userId+ is a list of users AWS accounts ids. Returns +true+ or an exception.
      #
      #  ec2.modify_image_launch_perm_remove_users('ami-e444444d',['000000000777','000000000778']) #=> true
      #
    def modify_image_launch_perm_remove_users(image_id, user_id=[])
      modify_image_attribute(image_id, 'launchPermission', 'remove', :user_id => user_id.to_a)
    end

      # Add image launch permissions for users groups (currently only 'all' is supported, which gives public launch permissions). 
      # Returns +true+ or an exception.
      #
      #  ec2.modify_image_launch_perm_add_groups('ami-e444444d') #=> true
      #
    def modify_image_launch_perm_add_groups(image_id, user_group=['all'])
      modify_image_attribute(image_id, 'launchPermission', 'add', :user_group => user_group.to_a)
    end
    
      # Remove image launch permissions for users groups (currently only 'all' is supported, which gives public launch permissions). 
      #
      #  ec2.modify_image_launch_perm_remove_groups('ami-e444444d') #=> true
      #
    def modify_image_launch_perm_remove_groups(image_id, user_group=['all'])
      modify_image_attribute(image_id, 'launchPermission', 'remove', :user_group => user_group.to_a)
    end
    
      # Add product code to image
      #
      #  ec2.modify_image_product_code('ami-e444444d','0ABCDEF') #=> true
      #
    def modify_image_product_code(image_id, product_code=[])
      modify_image_attribute(image_id, 'productCodes', nil, :product_code => product_code.to_a)
    end

  #-----------------------------------------------------------------
  #      Instances
  #-----------------------------------------------------------------
    
    def get_desc_instances(instances)  # :nodoc:
      result = []
      instances.each do |reservation|
        reservation[:instances_set].each do |instance|
          # Parse and remove timestamp from the reason string. The timestamp is of
          # the request, not when EC2 took action, thus confusing & useless...
          instance[:aws_reason]         = instance[:aws_reason].sub(/\(\d[^)]*GMT\) */, '')
          instance[:aws_owner]          = reservation[:aws_owner]
          instance[:aws_reservation_id] = reservation[:aws_reservation_id]
          instance[:aws_groups]         = reservation[:aws_groups]
          result << instance
        end
      end
      result
    rescue Exception
      on_exception
    end
    
      # Retrieve information about EC2 instances. If +list+ is omitted then returns the
      # list of all instances.
      #
      #  ec2.describe_instances #=> 
      #    [{:aws_image_id       => "ami-e444444d",
      #      :aws_reason         => "",
      #      :aws_state_code     => "16",
      #      :aws_owner          => "000000000888",
      #      :aws_instance_id    => "i-123f1234",
      #      :aws_reservation_id => "r-aabbccdd",
      #      :aws_state          => "running",
      #      :dns_name           => "domU-12-34-67-89-01-C9.usma2.compute.amazonaws.com",
      #      :ssh_key_name       => "staging",
      #      :aws_groups         => ["default"],
      #      :private_dns_name   => "domU-12-34-67-89-01-C9.usma2.compute.amazonaws.com",
      #      :aws_instance_type  => "m1.small",
      #      :aws_launch_time    => "2008-1-1T00:00:00.000Z"},
      #      :aws_availability_zone => "us-east-1b",
      #      :aws_kernel_id      => "aki-ba3adfd3",
      #      :aws_ramdisk_id     => "ari-badbad00",
      #       ..., {...}]
      #
    def describe_instances(list=[])
      link = generate_request("DescribeInstances", hash_params('InstanceId',list.to_a))
      request_cache_or_info(:describe_instances, link,  QEc2DescribeInstancesParser, list.blank?) do |parser|
        get_desc_instances(parser.result)
      end
    rescue Exception
      on_exception
    end
    
      # Return the product code attached to instance or +nil+ otherwise.
      #
      #  ec2.confirm_product_instance('ami-e444444d','12345678') #=> nil
      #  ec2.confirm_product_instance('ami-e444444d','00001111') #=> "000000000888"
      #
    def confirm_product_instance(instance, product_code)
      link = generate_request("ConfirmProductInstance", { 'ProductCode' => product_code,
                                                          'InstanceId'  => instance })
      request_info(link, QEc2ConfirmProductInstanceParser.new(:logger => @logger))
    end
    
      # Launch new EC2 instances. Returns a list of launched instances or an exception.
      #
      #  ec2.run_instances('ami-e444444d',1,1,['my_awesome_group'],'my_awesome_key', 'Woohoo!!!', 'public') #=>
      #   [{:aws_image_id       => "ami-e444444d",
      #     :aws_reason         => "",
      #     :aws_state_code     => "0",
      #     :aws_owner          => "000000000888",
      #     :aws_instance_id    => "i-123f1234",
      #     :aws_reservation_id => "r-aabbccdd",
      #     :aws_state          => "pending",
      #     :dns_name           => "",
      #     :ssh_key_name       => "my_awesome_key",
      #     :aws_groups         => ["my_awesome_group"],
      #     :private_dns_name   => "",
      #     :aws_instance_type  => "m1.small",
      #     :aws_launch_time    => "2008-1-1T00:00:00.000Z"
      #     :aws_ramdisk_id     => "ari-8605e0ef"
      #     :aws_kernel_id      => "aki-9905e0f0",
      #     :ami_launch_index   => "0",
      #     :aws_availability_zone => "us-east-1b"
      #     }]
      #
    def run_instances(image_id, min_count, max_count, group_ids, key_name, user_data='',  
                      addressing_type = nil, instance_type = nil,
                      kernel_id = nil, ramdisk_id = nil, availability_zone = nil, 
                      block_device_mappings = nil) 
 	    launch_instances(image_id, { :min_count       => min_count, 
 	                                 :max_count       => max_count, 
 	                                 :user_data       => user_data, 
                                   :group_ids       => group_ids, 
                                   :key_name        => key_name, 
                                   :instance_type   => instance_type, 
                                   :addressing_type => addressing_type,
                                   :kernel_id       => kernel_id,
                                   :ramdisk_id      => ramdisk_id,
                                   :availability_zone     => availability_zone,
                                   :block_device_mappings => block_device_mappings
                                 }) 
    end
    
     
      # Launch new EC2 instances. Returns a list of launched instances or an exception. 
      #
      # +lparams+ keys (default values in parenthesis):
      #  :min_count              fixnum, (1)
      #  :max_count              fixnum, (1)
      #  :group_ids              array or string ([] == 'default')
      #  :instance_type          string (DEFAULT_INSTACE_TYPE)
      #  :addressing_type        string (DEFAULT_ADDRESSING_TYPE 
      #  :key_name               string
      #  :kernel_id              string
      #  :ramdisk_id             string 
      #  :availability_zone      string
      #  :block_device_mappings  string
      #  :user_data              string
      # 
      #  ec2.launch_instances('ami-e444444d', :group_ids => 'my_awesome_group', 
      #                                       :user_data => "Woohoo!!!", 
      #                                       :addressing_type => "public", 
      #                                       :key_name => "my_awesome_key", 
      #                                       :availability_zone => "us-east-1c") #=> 
      #   [{:aws_image_id       => "ami-e444444d", 
      #     :aws_reason         => "", 
      #     :aws_state_code     => "0", 
      #     :aws_owner          => "000000000888", 
      #     :aws_instance_id    => "i-123f1234", 
      #     :aws_reservation_id => "r-aabbccdd", 
      #     :aws_state          => "pending", 
      #     :dns_name           => "", 
      #     :ssh_key_name       => "my_awesome_key", 
      #     :aws_groups         => ["my_awesome_group"], 
      #     :private_dns_name   => "", 
      #     :aws_instance_type  => "m1.small",
      #     :aws_launch_time    => "2008-1-1T00:00:00.000Z",
      #     :aws_ramdisk_id     => "ari-8605e0ef"
      #     :aws_kernel_id      => "aki-9905e0f0",
      #     :ami_launch_index   => "0",
      #     :aws_availability_zone => "us-east-1c"
      #     }] 
      #     
    def launch_instances(image_id, lparams={}) 
      @logger.info("Launching instance of image #{image_id} for #{@aws_access_key_id}, " + 
                   "key: #{lparams[:key_name]}, groups: #{(lparams[:group_ids]).to_a.join(',')}")
      # careful: keyName and securityGroups may be nil
      params = hash_params('SecurityGroup', lparams[:group_ids].to_a)
      params.update( {'ImageId'        => image_id,
                      'MinCount'       => (lparams[:min_count] || 1).to_s, 
                      'MaxCount'       => (lparams[:max_count] || 1).to_s, 
                      'AddressingType' => lparams[:addressing_type] || DEFAULT_ADDRESSING_TYPE, 
                      'InstanceType'   => lparams[:instance_type]   || DEFAULT_INSTANCE_TYPE })
      # optional params
      params['KeyName']                    = lparams[:key_name]              unless lparams[:key_name].blank? 
      params['KernelId']                   = lparams[:kernel_id]             unless lparams[:kernel_id].blank? 
      params['RamdiskId']                  = lparams[:ramdisk_id]            unless lparams[:ramdisk_id].blank? 
      params['Placement.AvailabilityZone'] = lparams[:availability_zone]     unless lparams[:availability_zone].blank? 
      params['BlockDeviceMappings']        = lparams[:block_device_mappings] unless lparams[:block_device_mappings].blank?
      unless lparams[:user_data].blank? 
        lparams[:user_data].strip! 
          # Do not use CGI::escape(encode64(...)) as it is done in Amazons EC2 library.
          # Amazon 169.254.169.254 does not like escaped symbols!
          # And it doesn't like "\n" inside of encoded string! Grrr....
          # Otherwise, some of UserData symbols will be lost...
        params['UserData'] = Base64.encode64(lparams[:user_data]).delete("\n") unless lparams[:user_data].blank?
      end
      link = generate_request("RunInstances", params)
        #debugger
      instances = request_info(link, QEc2DescribeInstancesParser.new(:logger => @logger))
      get_desc_instances(instances)
    rescue Exception
      on_exception
    end
    
      # Terminates EC2 instances. Returns a list of termination params or an exception.
      #
      #  ec2.terminate_instances(['i-f222222d','i-f222222e']) #=>
      #    [{:aws_shutdown_state      => "shutting-down", 
      #      :aws_instance_id         => "i-f222222d", 
      #      :aws_shutdown_state_code => 32, 
      #      :aws_prev_state          => "running", 
      #      :aws_prev_state_code     => 16}, 
      #     {:aws_shutdown_state      => "shutting-down", 
      #      :aws_instance_id         => "i-f222222e", 
      #      :aws_shutdown_state_code => 32, 
      #      :aws_prev_state          => "running", 
      #      :aws_prev_state_code     => 16}]
      #
    def terminate_instances(list=[])
      link = generate_request("TerminateInstances", hash_params('InstanceId',list.to_a))
      request_info(link, QEc2TerminateInstancesParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end

      # Retreive EC2 instance OS logs. Returns a hash of data or an exception.
      #
      #  ec2.get_console_output('i-f222222d') =>
      #    {:aws_instance_id => 'i-f222222d',
      #     :aws_timestamp   => "2007-05-23T14:36:07.000-07:00",
      #     :timestamp       => Wed May 23 21:36:07 UTC 2007,          # Time instance
      #     :aws_output      => "Linux version 2.6.16-xenU (builder@patchbat.amazonsa) (gcc version 4.0.1 20050727 ..."
    def get_console_output(instance_id)
      link = generate_request("GetConsoleOutput", { 'InstanceId.1' => instance_id })
      request_info(link, QEc2GetConsoleOutputParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end

      # Reboot an EC2 instance. Returns +true+ or an exception.
      #
      #  ec2.reboot_instances(['i-f222222d','i-f222222e']) #=> true
      #
    def reboot_instances(list)
      link = generate_request("RebootInstances", hash_params('InstanceId', list.to_a))
      request_info(link, RightBoolResponseParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end
    
  #-----------------------------------------------------------------
  #      Security groups
  #-----------------------------------------------------------------

      # Retrieve Security Group information. If +list+ is omitted the returns the whole list of groups.
      #
      #  ec2.describe_security_groups #=>
      #    [{:aws_group_name  => "default-1",
      #      :aws_owner       => "000000000888",
      #      :aws_description => "Default allowing SSH, HTTP, and HTTPS ingress",
      #      :aws_perms       =>
      #        [{:owner => "000000000888", :group => "default"},
      #         {:owner => "000000000888", :group => "default-1"},
      #         {:to_port => "-1",  :protocol => "icmp", :from_port => "-1",  :cidr_ips => "0.0.0.0/0"},
      #         {:to_port => "22",  :protocol => "tcp",  :from_port => "22",  :cidr_ips => "0.0.0.0/0"},
      #         {:to_port => "80",  :protocol => "tcp",  :from_port => "80",  :cidr_ips => "0.0.0.0/0"},
      #         {:to_port => "443", :protocol => "tcp",  :from_port => "443", :cidr_ips => "0.0.0.0/0"}]},
      #    ..., {...}]
      #
    def describe_security_groups(list=[])
      link = generate_request("DescribeSecurityGroups", hash_params('GroupName',list.to_a))
      request_cache_or_info( :describe_security_groups, link,  QEc2DescribeSecurityGroupsParser, list.blank?) do |parser|
        result = []     
        parser.result.each do |item|
          perms = []
          item.ipPermissions.each do |perm|
            perm.groups.each do |ngroup|
              perms << {:group => ngroup.groupName,
                        :owner => ngroup.userId}
            end
            perm.ipRanges.each do |cidr_ip|
              perms << {:from_port => perm.fromPort, 
                        :to_port   => perm.toPort, 
                        :protocol  => perm.ipProtocol,
                        :cidr_ips  => cidr_ip}
            end
          end

             # delete duplication
          perms.each_index do |i|
            (0...i).each do |j|
              if perms[i] == perms[j] then perms[i] = nil; break; end
            end
          end
          perms.compact!

          result << {:aws_owner       => item.ownerId, 
                     :aws_group_name  => item.groupName, 
                     :aws_description => item.groupDescription,
                     :aws_perms       => perms}
        
        end
        result
      end
    rescue Exception
      on_exception
    end
    
      # Create new Security Group. Returns +true+ or an exception.
      #
      #  ec2.create_security_group('default-1',"Default allowing SSH, HTTP, and HTTPS ingress") #=> true
      #
    def create_security_group(name, description)
      # EC2 doesn't like an empty description...
      description = " " if description.blank?
      link = generate_request("CreateSecurityGroup", 
                              'GroupName'        => name.to_s, 
                              'GroupDescription' => description.to_s)
      request_info(link, RightBoolResponseParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end

      # Remove Security Group. Returns +true+ or an exception.
      #
      #  ec2.delete_security_group('default-1') #=> true
      #
    def delete_security_group(name)
      link = generate_request("DeleteSecurityGroup", 
                              'GroupName' => name.to_s)
      request_info(link, RightBoolResponseParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end
    
      # Authorize named ingress for security group. Allows instances that are member of someone
      # else's security group to open connections to instances in my group.
      #
      #  ec2.authorize_security_group_named_ingress('my_awesome_group', '7011-0219-8268', 'their_group_name') #=> true
      #
    def authorize_security_group_named_ingress(name, owner, group)
      link = generate_request("AuthorizeSecurityGroupIngress", 
                              'GroupName'                  => name.to_s, 
                              'SourceSecurityGroupName'    => group.to_s, 
                              'SourceSecurityGroupOwnerId' => owner.to_s.gsub(/-/,''))
      request_info(link, RightBoolResponseParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end
    
      # Revoke named ingress for security group.
      #
      #  ec2.revoke_security_group_named_ingress('my_awesome_group', aws_user_id, 'another_group_name') #=> true
      #
    def revoke_security_group_named_ingress(name, owner, group)
      link = generate_request("RevokeSecurityGroupIngress", 
                              'GroupName'                  => name.to_s, 
                              'SourceSecurityGroupName'    => group.to_s, 
                              'SourceSecurityGroupOwnerId' => owner.to_s.gsub(/-/,''))
      request_info(link, RightBoolResponseParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end
    
      # Add permission to a security group. Returns +true+ or an exception. +protocol+ is one of :'tcp'|'udp'|'icmp'.
      #
      #  ec2.authorize_security_group_IP_ingress('my_awesome_group', 80, 82, 'udp', '192.168.1.0/8') #=> true
      #  ec2.authorize_security_group_IP_ingress('my_awesome_group', -1, -1, 'icmp') #=> true
      #
    def authorize_security_group_IP_ingress(name, from_port, to_port, protocol='tcp', cidr_ip='0.0.0.0/0')
      link = generate_request("AuthorizeSecurityGroupIngress", 
                              'GroupName'  => name.to_s, 
                              'IpProtocol' => protocol.to_s, 
                              'FromPort'   => from_port.to_s, 
                              'ToPort'     => to_port.to_s, 
                              'CidrIp'     => cidr_ip.to_s)
      request_info(link, RightBoolResponseParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end
    
      # Remove permission from a security group. Returns +true+ or an exception. +protocol+ is one of :'tcp'|'udp'|'icmp' ('tcp' is default). 
      #
      #  ec2.revoke_security_group_IP_ingress('my_awesome_group', 80, 82, 'udp', '192.168.1.0/8') #=> true
      #
    def revoke_security_group_IP_ingress(name, from_port, to_port, protocol='tcp', cidr_ip='0.0.0.0/0')
      link = generate_request("RevokeSecurityGroupIngress", 
                              'GroupName'  => name.to_s, 
                              'IpProtocol' => protocol.to_s, 
                              'FromPort'   => from_port.to_s, 
                              'ToPort'     => to_port.to_s, 
                              'CidrIp'     => cidr_ip.to_s)
      request_info(link, RightBoolResponseParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end

  #-----------------------------------------------------------------
  #      Keys
  #-----------------------------------------------------------------
  
      # Retrieve a list of SSH keys. Returns an array of keys or an exception. Each key is
      # represented as a two-element hash.
      #
      #  ec2.describe_key_pairs #=>
      #    [{:aws_fingerprint=> "01:02:03:f4:25:e6:97:e8:9b:02:1a:26:32:4e:58:6b:7a:8c:9f:03", :aws_key_name=>"key-1"},
      #     {:aws_fingerprint=> "1e:29:30:47:58:6d:7b:8c:9f:08:11:20:3c:44:52:69:74:80:97:08", :aws_key_name=>"key-2"},
      #      ..., {...} ]
      #
    def describe_key_pairs(list=[])
      link = generate_request("DescribeKeyPairs", hash_params('KeyName',list.to_a))
      request_cache_or_info :describe_key_pairs, link,  QEc2DescribeKeyPairParser, list.blank?
    rescue Exception
      on_exception
    end
      
      # Create new SSH key. Returns a hash of the key's data or an exception.
      #
      #  ec2.create_key_pair('my_awesome_key') #=>
      #    {:aws_key_name    => "my_awesome_key",
      #     :aws_fingerprint => "01:02:03:f4:25:e6:97:e8:9b:02:1a:26:32:4e:58:6b:7a:8c:9f:03",
      #     :aws_material    => "-----BEGIN RSA PRIVATE KEY-----\nMIIEpQIBAAK...Q8MDrCbuQ=\n-----END RSA PRIVATE KEY-----"}
      #
    def create_key_pair(name)
      link = generate_request("CreateKeyPair", 
                              'KeyName' => name.to_s)
      request_info(link, QEc2CreateKeyPairParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end
      
      # Delete a key pair. Returns +true+ or an exception.
      #
      #  ec2.delete_key_pair('my_awesome_key') #=> true
      #
    def delete_key_pair(name)
      link = generate_request("DeleteKeyPair", 
                              'KeyName' => name.to_s)
      request_info(link, RightBoolResponseParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end
    
  #-----------------------------------------------------------------
  #      Elastic IPs
  #-----------------------------------------------------------------

    # Acquire a new elastic IP address for use with your account.
    # Returns allocated IP address or an exception.
    #
    #  ec2.allocate_address #=> '75.101.154.140'
    #
    def allocate_address
      link = generate_request("AllocateAddress")
      request_info(link, QEc2AllocateAddressParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end

    # Associate an elastic IP address with an instance.
    # Returns +true+ or an exception.
    #
    #  ec2.associate_address('i-d630cbbf', '75.101.154.140') #=> true
    #
    def associate_address(instance_id, public_ip)
      link = generate_request("AssociateAddress", 
                              "InstanceId" => instance_id.to_s,
                              "PublicIp"   => public_ip.to_s)
      request_info(link, RightBoolResponseParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end

    # List elastic IP addresses assigned to your account.
    # Returns an array of 2 keys (:instance_id and :public_ip) hashes:
    #
    #  ec2.describe_addresses  #=> [{:instance_id=>"i-d630cbbf", :public_ip=>"75.101.154.140"},
    #                               {:instance_id=>nil, :public_ip=>"75.101.154.141"}]
    #
    #  ec2.describe_addresses('75.101.154.140') #=> [{:instance_id=>"i-d630cbbf", :public_ip=>"75.101.154.140"}]
    #
    def describe_addresses(list=[])
      link = generate_request("DescribeAddresses", 
                              hash_params('PublicIp',list.to_a))
      request_cache_or_info :describe_addresses, link,  QEc2DescribeAddressesParser, list.blank?
    rescue Exception
      on_exception
    end

    # Disassociate the specified elastic IP address from the instance to which it is assigned.
    # Returns +true+ or an exception.
    # 
    #  ec2.disassociate_address('75.101.154.140') #=> true
    #
    def disassociate_address(public_ip)
      link = generate_request("DisassociateAddress", 
                              "PublicIp" => public_ip.to_s)
      request_info(link, RightBoolResponseParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end

    # Release an elastic IP address associated with your account.
    # Returns +true+ or an exception.
    #
    #  ec2.release_address('75.101.154.140') #=> true
    #
    def release_address(public_ip)
      link = generate_request("ReleaseAddress", 
                              "PublicIp" => public_ip.to_s)
      request_info(link, RightBoolResponseParser.new(:logger => @logger))
    rescue Exception
      on_exception
    end

  #-----------------------------------------------------------------
  #      Availability zones
  #-----------------------------------------------------------------
    
    # Describes availability zones that are currently available to the account and their states.
    # Returns an array of 2 keys (:zone_name and :zone_state) hashes:
    #
    #  ec2.describe_availability_zones  #=> [{:zone_state=>"available", :zone_name=>"us-east-1a"}, 
    #                                        {:zone_state=>"available", :zone_name=>"us-east-1b"}, 
    #                                        {:zone_state=>"available", :zone_name=>"us-east-1c"}]
    #
    #  ec2.describe_availability_zones('us-east-1c') #=> [{:zone_state=>"available", :zone_name=>"us-east-1c"}]
    #
    def describe_availability_zones(list=[])
      link = generate_request("DescribeAvailabilityZones", 
                              hash_params('ZoneName',list.to_a))
      request_cache_or_info :describe_availability_zones, link,  QEc2DescribeAvailabilityZonesParser, list.blank?
    rescue Exception
      on_exception
    end

  
    
  #-----------------------------------------------------------------
  #      PARSERS: Boolean Response Parser
  #-----------------------------------------------------------------
    
    class RightBoolResponseParser < RightAWSParser #:nodoc:
      def tagend(name)
        @result = @text=='true' ? true : false if name == 'return'
      end
    end

  #-----------------------------------------------------------------
  #      PARSERS: Key Pair
  #-----------------------------------------------------------------

    class QEc2DescribeKeyPairParser < RightAWSParser #:nodoc:
      def tagstart(name, attributes)
        @item = {} if name == 'item'
      end
      def tagend(name)
        case name 
          when 'keyName'        then @item[:aws_key_name]    = @text
          when 'keyFingerprint' then @item[:aws_fingerprint] = @text
          when 'item'           then @result                << @item
        end
      end
      def reset
        @result = [];    
      end
    end

    class QEc2CreateKeyPairParser < RightAWSParser #:nodoc:
      def tagstart(name, attributes)
        @result = {} if name == 'CreateKeyPairResponse'
      end
      def tagend(name)
        case name 
          when 'keyName'        then @result[:aws_key_name]    = @text
          when 'keyFingerprint' then @result[:aws_fingerprint] = @text
          when 'keyMaterial'    then @result[:aws_material]    = @text
        end
      end
    end

  #-----------------------------------------------------------------
  #      PARSERS: Security Groups
  #-----------------------------------------------------------------

    class QEc2UserIdGroupPairType #:nodoc:
      attr_accessor :userId
      attr_accessor :groupName
    end

    class QEc2IpPermissionType #:nodoc:
      attr_accessor :ipProtocol
      attr_accessor :fromPort
      attr_accessor :toPort
      attr_accessor :groups
      attr_accessor :ipRanges
    end

    class QEc2SecurityGroupItemType #:nodoc:
      attr_accessor :groupName
      attr_accessor :groupDescription
      attr_accessor :ownerId
      attr_accessor :ipPermissions
    end


    class QEc2DescribeSecurityGroupsParser < RightAWSParser #:nodoc:
      def tagstart(name, attributes)
        case name
          when 'item' 
            if @xmlpath=='DescribeSecurityGroupsResponse/securityGroupInfo'
              @group = QEc2SecurityGroupItemType.new 
              @group.ipPermissions = []
            elsif @xmlpath=='DescribeSecurityGroupsResponse/securityGroupInfo/item/ipPermissions'
              @perm = QEc2IpPermissionType.new
              @perm.ipRanges = []
              @perm.groups   = []
            elsif @xmlpath=='DescribeSecurityGroupsResponse/securityGroupInfo/item/ipPermissions/item/groups'
              @sgroup = QEc2UserIdGroupPairType.new
            end
        end
      end
      def tagend(name)
        case name
          when 'ownerId'          then @group.ownerId   = @text
          when 'groupDescription' then @group.groupDescription = @text
          when 'groupName'
            if @xmlpath=='DescribeSecurityGroupsResponse/securityGroupInfo/item'
              @group.groupName  = @text 
            elsif @xmlpath=='DescribeSecurityGroupsResponse/securityGroupInfo/item/ipPermissions/item/groups/item'
              @sgroup.groupName = @text 
            end
          when 'ipProtocol'       then @perm.ipProtocol = @text
          when 'fromPort'         then @perm.fromPort   = @text
          when 'toPort'           then @perm.toPort     = @text
          when 'userId'           then @sgroup.userId   = @text
          when 'cidrIp'           then @perm.ipRanges  << @text
          when 'item'
            if @xmlpath=='DescribeSecurityGroupsResponse/securityGroupInfo/item/ipPermissions/item/groups'
              @perm.groups << @sgroup
            elsif @xmlpath=='DescribeSecurityGroupsResponse/securityGroupInfo/item/ipPermissions'
              @group.ipPermissions << @perm
            elsif @xmlpath=='DescribeSecurityGroupsResponse/securityGroupInfo'
              @result << @group
            end
        end
      end
      def reset
        @result = []
      end
    end

  #-----------------------------------------------------------------
  #      PARSERS: Images
  #-----------------------------------------------------------------
    
    class QEc2DescribeImagesParser < RightAWSParser #:nodoc:
      def tagstart(name, attributes)
        if name == 'item' && @xmlpath[%r{.*/imagesSet$}]
          @image = {}
        end
      end
      def tagend(name)
        case name
          when 'imageId'       then @image[:aws_id]       = @text
          when 'imageLocation' then @image[:aws_location] = @text
          when 'imageState'    then @image[:aws_state]    = @text
          when 'imageOwnerId'  then @image[:aws_owner]    = @text
          when 'isPublic'      then @image[:aws_is_public]= @text == 'true' ? true : false
          when 'productCode'   then (@image[:aws_product_codes] ||= []) << @text
          when 'architecture'  then @image[:aws_architecture] = @text
          when 'imageType'     then @image[:aws_image_type] = @text
          when 'kernelId'      then @image[:aws_kernel_id]  = @text
          when 'ramdiskId'     then @image[:aws_ramdisk_id] = @text
          when 'item'          then @result << @image if @xmlpath[%r{.*/imagesSet$}]
        end
      end
      def reset
        @result = []
      end
    end

    class QEc2RegisterImageParser < RightAWSParser #:nodoc:
      def tagend(name)
        @result = @text if name == 'imageId'
      end
    end

  #-----------------------------------------------------------------
  #      PARSERS: Image Attribute
  #-----------------------------------------------------------------

    class QEc2DescribeImageAttributeParser < RightAWSParser #:nodoc:
      def tagstart(name, attributes)
        case name
          when 'launchPermission'
            @result[:groups] = []
            @result[:users]  = []
          when 'productCodes'
            @result[:aws_product_codes] = []
        end
      end
      def tagend(name)
          # right now only 'launchPermission' is supported by Amazon. 
          # But nobody know what will they xml later as attribute. That is why we 
          # check for 'group' and 'userId' inside of 'launchPermission/item'
        case name
          when 'imageId'            then @result[:aws_id] = @text
          when 'group'              then @result[:groups] << @text if @xmlpath == 'DescribeImageAttributeResponse/launchPermission/item'
          when 'userId'             then @result[:users]  << @text if @xmlpath == 'DescribeImageAttributeResponse/launchPermission/item'
          when 'productCode'        then @result[:aws_product_codes] << @text
          when 'kernel'             then @result[:aws_kernel]  = @text
          when 'ramdisk'            then @result[:aws_ramdisk] = @text
          when 'blockDeviceMapping' then @result[:block_device_mapping] = @text
        end
      end
      def reset
        @result = {}
      end
    end

  #-----------------------------------------------------------------
  #      PARSERS: Instances
  #-----------------------------------------------------------------

    class QEc2DescribeInstancesParser < RightAWSParser #:nodoc:
      def tagstart(name, attributes)
           # DescribeInstances property
        if (name == 'item' && @xmlpath == 'DescribeInstancesResponse/reservationSet') || 
           # RunInstances property
           (name == 'RunInstancesResponse')  
            @reservation = { :aws_groups    => [],
                             :instances_set => [] }
              
        elsif (name == 'item') && 
                # DescribeInstances property
              ( @xmlpath=='DescribeInstancesResponse/reservationSet/item/instancesSet' ||
               # RunInstances property
                @xmlpath=='RunInstancesResponse/instancesSet' )
              # the optional params (sometimes are missing and we dont want them to be nil) 
            @instance = { :aws_reason       => '',
                          :dns_name         => '',
                          :private_dns_name => '',
                          :ami_launch_index => '',
                          :ssh_key_name     => '',
                          :aws_state        => '',
                          :aws_product_codes => [] }
        end
      end
      def tagend(name)
        case name 
          # reservation
          when 'reservationId'    then @reservation[:aws_reservation_id] = @text
          when 'ownerId'          then @reservation[:aws_owner]          = @text
          when 'groupId'          then @reservation[:aws_groups]        << @text
          # instance  
          when 'instanceId'       then @instance[:aws_instance_id]    = @text
          when 'imageId'          then @instance[:aws_image_id]       = @text
          when 'dnsName'          then @instance[:dns_name]           = @text
          when 'privateDnsName'   then @instance[:private_dns_name]   = @text
          when 'reason'           then @instance[:aws_reason]         = @text
          when 'keyName'          then @instance[:ssh_key_name]       = @text
          when 'amiLaunchIndex'   then @instance[:ami_launch_index]   = @text
          when 'code'             then @instance[:aws_state_code]     = @text
          when 'name'             then @instance[:aws_state]          = @text
          when 'productCode'      then @instance[:aws_product_codes] << @text
          when 'instanceType'     then @instance[:aws_instance_type]  = @text
          when 'launchTime'       then @instance[:aws_launch_time]    = @text
          when 'kernelId'         then @instance[:aws_kernel_id]      = @text
          when 'ramdiskId'        then @instance[:aws_ramdisk_id]     = @text
          when 'availabilityZone' then @instance[:aws_availability_zone] = @text
          when 'item'
            if @xmlpath == 'DescribeInstancesResponse/reservationSet/item/instancesSet' || # DescribeInstances property
               @xmlpath == 'RunInstancesResponse/instancesSet'            # RunInstances property
              @reservation[:instances_set] << @instance
            elsif @xmlpath=='DescribeInstancesResponse/reservationSet'    # DescribeInstances property
              @result << @reservation
            end
          when 'RunInstancesResponse' then @result << @reservation            # RunInstances property
        end
      end
      def reset
        @result = []
      end
    end

    class QEc2ConfirmProductInstanceParser < RightAWSParser #:nodoc:
      def tagend(name)
        @result = @text if name == 'ownerId'
      end
    end

    class QEc2TerminateInstancesParser < RightAWSParser #:nodoc:
      def tagstart(name, attributes)
        @instance = {} if name == 'item'
      end
      def tagend(name)
        case name
        when 'instanceId' then @instance[:aws_instance_id] = @text
        when 'code'
          if @xmlpath == 'TerminateInstancesResponse/instancesSet/item/shutdownState'
               @instance[:aws_shutdown_state_code] = @text.to_i
          else @instance[:aws_prev_state_code]     = @text.to_i end
        when 'name'
          if @xmlpath == 'TerminateInstancesResponse/instancesSet/item/shutdownState'
               @instance[:aws_shutdown_state] = @text
          else @instance[:aws_prev_state]     = @text end
        when 'item'       then @result << @instance
        end
      end
      def reset
        @result = []
      end
    end

  #-----------------------------------------------------------------
  #      PARSERS: Console
  #-----------------------------------------------------------------

    class QEc2GetConsoleOutputParser < RightAWSParser #:nodoc:
      def tagend(name)
        case name
        when 'instanceId' then @result[:aws_instance_id] = @text
        when 'timestamp'  then @result[:aws_timestamp]   = @text
                               @result[:timestamp]       = (Time.parse(@text)).utc
        when 'output'     then @result[:aws_output]      = Base64.decode64(@text)
        end
      end
      def reset
        @result = {}
      end
    end

  #-----------------------------------------------------------------
  #      PARSERS: Fake
  #-----------------------------------------------------------------
  
    # Dummy parser - does nothing
    # Returns the original params back
    class QEc2DummyParser  # :nodoc:
      attr_accessor :result
      def parse(response, params={})
        @result = [response, params]
      end
    end
    
  #-----------------------------------------------------------------
  #      PARSERS: Elastic IPs
  #-----------------------------------------------------------------
  
    class QEc2AllocateAddressParser < RightAWSParser #:nodoc:
      def tagend(name)
        @result = @text if name == 'publicIp'
      end
    end
    
    class QEc2DescribeAddressesParser < RightAWSParser #:nodoc:
      def tagstart(name, attributes)
        @address = {} if name == 'item'
      end
      def tagend(name)
        case name
        when 'instanceId' then @address[:instance_id] = @text.blank? ? nil : @text 
        when 'publicIp'   then @address[:public_ip]   = @text
        when 'item'       then @result << @address
        end
      end
      def reset
        @result = []
      end
    end

  #-----------------------------------------------------------------
  #      PARSERS: AvailabilityZones
  #-----------------------------------------------------------------

    class QEc2DescribeAvailabilityZonesParser < RightAWSParser #:nodoc:
      def tagstart(name, attributes)
        @zone = {} if name == 'item'
      end
      def tagend(name)
        case name
        when 'zoneName'  then @zone[:zone_name]  = @text
        when 'zoneState' then @zone[:zone_state] = @text
        when 'item'      then @result << @zone
        end
      end
      def reset
        @result = []
      end
    end

 
    
  end
      
end
