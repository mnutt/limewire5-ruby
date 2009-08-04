get '/' do
  "yo"
end

get '/hey' do
  "hi"
end

get /.*/ do
  params.inspect
end

