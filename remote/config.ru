root = './remote/'
$: << File.join(root, 'vendor/sinatra/lib')

require 'sinatra'
set :environment, 'development'
set :root, root
disable :run

app = lambda do |env|
  Sinatra::Application.reset!

  load File.join(root, 'app.rb')

  Sinatra::Application.call(env)
end

run app
