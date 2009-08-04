# The Limewire library is a Ruby interface for the LimeWire peer-to-peer client.  It exposes various
# LimeWire services such as searching, downloading, and library management.  In this early version, the
# library must be instantiated from JRuby running as a service inside of LimeWire.
#
# Note: there is somewhat inconsistent use of URN and SHA1 hashes.  URNs are of the form
# <tt>urn:sha1:823A3F24DF34012BB35823A3F24DF34012BB35</tt>, wherease SHA1s are of the form
# <tt>823A3F24DF34012BB35823A3F24DF34012BB35</tt>

require 'limewire/filterable'
require 'limewire/library'
require 'limewire/download'
require 'limewire/search'
require 'limewire/mojito'

# The Limewire module should be the sole method used to interact with LimeWire's core.
module Limewire
  # The global variable $injector is passed in from LimeWire.  It should not be used outside of the Limewire
  # gem, if possible.
  def self.injector
    @injector ||= $injector rescue nil
  end

  # The length of time the LimeWire client has been running, in seconds.
  def self.uptime
    Core::Statistics.uptime / 1000
  end

  def self.my_guid
    Core::GUID.new(Core::ApplicationServices.get_my_guid)
  end

  # The average length of time the LimeWire has been run so far today.
  def self.daily_uptime
    Core::Statistics.calculate_daily_uptime
  end
end
