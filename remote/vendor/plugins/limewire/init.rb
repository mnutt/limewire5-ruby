$LOAD_PATH.unshift File.dirname(__FILE__)
require 'lib/limewire'

ActiveSupport::Dependencies.load_once_paths.delete(ActiveSupport::Dependencies.load_once_paths.find{|v| v["limewire/"]})
ActiveSupport::Dependencies.explicitly_unloadable_constants << 'Limewire'
