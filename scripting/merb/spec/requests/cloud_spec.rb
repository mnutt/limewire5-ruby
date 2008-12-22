require File.join(File.dirname(__FILE__), '..', 'spec_helper.rb')

describe "/cloud" do
  before(:each) do
    @response = request("/cloud")
  end
end