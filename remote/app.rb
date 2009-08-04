root = './remote/'
$: << File.join(root, 'vendor/sinatra/lib')

require 'sinatra'
set :environment, 'development'
set :root,        root
disable :run

run Sinatra::Application

###
###  End rack schtuff
###

get '/' do
  "yo"
end

get '/hey' do
  "hi"
end

get /.*/ do
  params.inspect
end

