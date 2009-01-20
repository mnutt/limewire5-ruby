# This file is auto-generated from the current state of the database. Instead of editing this file, 
# please use the migrations feature of Active Record to incrementally modify your database, and
# then regenerate this schema definition.
#
# Note that this schema.rb definition is the authoritative source for your database schema. If you need
# to create the application database on another system, you should be using db:schema:load, not running
# all the migrations from scratch. The latter is a flawed and unsustainable approach (the more migrations
# you'll amass, the slower it'll run and the greater likelihood for issues).
#
# It's strongly recommended to check this file into your version control system.

ActiveRecord::Schema.define(:version => 20090119172145) do

  create_table "dashboard_positions", :force => true do |t|
    t.integer   "column"
    t.integer   "list_position"
    t.string    "name"
    t.timestamp "created_at"
    t.timestamp "updated_at"
  end

  create_table "playlists", :force => true do |t|
    t.string    "name"
    t.integer   "collaborative"
    t.string    "tracks"
    t.integer   "smart"
    t.string    "share_hash"
    t.string    "version"
    t.integer   "owner_id"
    t.string    "genres"
    t.string    "artist"
    t.string    "tags"
    t.timestamp "uploaded_from"
    t.timestamp "uploaded_to"
    t.integer   "bpm_from"
    t.integer   "bpm_to"
    t.string    "search_term"
    t.string    "user_favorites"
    t.string    "list_order"
    t.integer   "duration_from"
    t.integer   "duration_to"
    t.timestamp "created_at"
    t.timestamp "updated_at"
    t.integer   "list_position"
  end

end
