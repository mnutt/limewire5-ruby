root = './remote/'
app = '/Users/jcamerer/lime/LimeWire5/remote/app.rb'

$: << File.join(root, 'vendor/sinatra/lib')

puts File.exist?(app)

require 'sinatra'

get '/' do
  "yo"
end

get '/hey' do
  "hi"
end

get /.*/ do
  params.inspect
end

set :environment, 'development'
set :root,        root
#set :app_file,    app
disable :run

run Sinatra::Application
