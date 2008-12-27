require File.join(File.dirname(__FILE__), '..', 'spec_helper.rb')

describe "/library" do
  before(:each) do
    @response = request("/library")
  end
end