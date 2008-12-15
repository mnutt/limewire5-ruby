require File.expand_path(File.dirname(__FILE__) + "/../francis")
require 'test/unit'

class Francis_Test < Test::Unit::TestCase
  def setup
    @fr = Francis.new do
      setup do
        @setup_called = true
      end

      dispatcher do
        @dispatcher_called = true
        @uri = @request
      end

      responder do
        @responder_called = true
        @response
      end
      
      get '/' do
        response.body="HOWDY"
      end

      get '/template.test' do
        @var = 'erbby'
        response.body = erb("<%=@var%>")
      end

      get %r{/script/how(.*)} do
        response.body = request.user_data[:match][0]
      end
    end
    
    @response = @fr.dispatch("/")
  end

  def test_default_content_type
    assert_equal "text/html", @response.content_type
  end

  def test_response_body
    assert_equal "HOWDY", @response.body
  end

  def test_multiple_dispatches
    response2 = @fr.dispatch("/")
    assert_equal "HOWDY", response2.body
  end

  def test_erb
    response = @fr.dispatch("/template.test") 
    assert_equal "erbby", response.body
  end

  def test_routing_error
    assert_raise Francis::RoutingError do
      @fr.dispatch("/i do not exist")
    end
  end

  def test_regex
    assert_equal "areyou", @fr.dispatch("/script/howareyou").body
  end

  def test_setup
    assert @fr.instance_variables.member?("@setup_called")
  end
  
  def test_dispatcher_proc
    assert @fr.instance_variables.member?("@dispatcher_called")
  end

  def test_responder_proc
    assert @fr.instance_variables.member?("@responder_called")
  end
end


