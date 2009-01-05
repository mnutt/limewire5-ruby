class CreatePlaylists < ActiveRecord::Migration
  def self.up
    create_table :playlists do |t|
      t.string :name
      t.integer :collaborative
      t.string :tracks
      t.integer :smart
      t.string :share_hash
      t.string :version
      t.integer :owner_id
      t.string :genres
      t.stirng :artist
      t.string :tags
      t.datetime :uploaded_from
      t.datetime :uploaded_to
      t.integer :bpm_from
      t.integer :bpm_to
      t.string :search_term
      t.string :user_favorites
      t.string :order
      t.integer :duration_from
      t.integer :duration-to

      t.timestamps
    end
  end

  def self.down
    drop_table :playlists
  end
end
