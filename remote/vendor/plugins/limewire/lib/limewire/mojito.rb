module Limewire
  # Mojito is LimeWire's distributed hash table implementation.  It is a large table of
  # key-value pairs that is shared among the nodes on the network.
  class Mojito
    # The name of the hash table
    def self.name
      Core::MojitoManager.name
    end

    # Are we able to connect to the DHT?
    def self.running?
      Core::MojitoManager.running?
    end

    # Send a command to the DHT. To see the output of the command, provide a block.
    #
    # Example:
    #
    #   >> LimeWire::Mojito.handle("help") do |output|
    #   *>   puts output
    #   *> end
    #
    def self.handle(command, &block)
      writer = RubyWriter.new(&block)
      Core::MojitoManager.handle(command, writer)
    end

    class RubyWriter < java.io.PrintWriter
      def initialize(&block)
        @block = block
      end

      def print(line)
        @block.call line
      end

      def println(line)
        print(line)
      end
    end
  end
end
