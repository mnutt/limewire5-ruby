class Playlist < ActiveRecord::Base
  before_create :generate_hash
  
  def to_playlist
    { :name => self.name,
      :collaborative => self.collaborative == 1,
      :smart_filter => {
        :genres => self.genres,
        :user_favorites => self.user_favorites,
        :tags => self.tags,
        :uploaded_from => self.uploaded_from,
        :uploaded_to => self.uploaded_to,
        :bpm_to => self.bpm_to,
        :bpm_from => self.bpm_from,
        :duration_to => self.duration_to,
        :duration_from => self.duration_from,
        :search_term => self.search_term,
        :artist => self.artist,
        :order => self.list_order,  # different because of sqlite
      },
      :smart => self.smart == 1,
      :tracks => self.tracks,
      :version => self.version,
      :owner => { :nickname => "Owner" },
      :date_created => self.created_at,
      :id => self.id,
      :hash => self.share_hash
    }
  end

  def generate_hash
    self.share_hash = Digest::MD5.new.to_s
  end
end
