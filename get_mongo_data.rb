require 'rubygems'
require 'mongo'

include Mongo

# https://gist.github.com/SkyM/e01635973b3f025eb992

SERVERS = {
  web: {
    host: 'centzy-production.m0.mongolayer.com',
    port: '27017',
    username: 'centzy_web',
    password: '4FZVvZKMUq43jmJsBUp9KLpLDHNnR9KdEEAi7p3X'
  },
  businesses: {
    host: 'locality-web-production-businesses.m0.mongolayer.com',
    port: '27017',
    username: 'locality_web',
    password: 'Mors7G4fQq8P2LkugtaJngKi2jf9seZLgAb2GanyGWWqMEoX9J'
  },
  areas: {
    host: 'locality-web-production-areas.m0.mongolayer.com',
    port: '27017',
    username: 'locality_web',
    password: 'Mors7G4fQq8P2LkugtaJngKi2jf9seZLgAb2GanyGWWqMEoX9J'
  }
}

DBS = {
  web: {
    server: SERVERS[:web],
    remote_name: 'centzy_web_production',
    local_name: 'centzy2_development',
    collections: %w(business_categories items)
    # collections: %w(ads appointment_business_data appointment_report appointment_searches appointments
    #   bookings business_categories business_major_categories citygrid_place_searches contacts
    #   delayed_backend_mongoid_jobs experiments item_categories items learn_contents legacy_urls
    #   lifepass_users merchant_leads merchant_menus messages neighborhoods neighborhoods2 promos
    #   referrers seo_business_categories seo_items seo_regions states system.indexes system.users
    #   user_report users yelp_api_businesses zip_codes
    # )
  },
  businesses: {
    server: SERVERS[:businesses],
    remote_name: 'locality_web_production_businesses',
    local_name:  'locality_web_development_businesses',
    collections: ['businesses']
  },
  areas: {
    server: SERVERS[:areas],
    remote_name: 'locality_web_production_areas',
    local_name:  'locality_web_development_areas',
    collections: %w(lists locations)
    # collections: %w(areas learn_contents lists locations)
  }
}

module Dumper

  # currently unused
  MAX_DOCS = 1_000

  def self.dump_remote()
    # dump_entire_db(:web)

    cmd_str = dump_cmd_str(:web, 'business_categories')
    puts cmd_str
    system(cmd_str)
    
    cmd_str = dump_cmd_str(:web, 'items')
    puts cmd_str
    system(cmd_str)
    
    # dump_last_n_docs(:businesses, 'businesses', MAX_DOCS)
    cmd_str = dump_cmd_str(:businesses, 'businesses', '{state: "NY"}')
    puts cmd_str
    system(cmd_str)
    
    # DBS[:areas][:collections].each do |collection_name|
    dump_last_n_docs(:areas, 'areas', MAX_DOCS)
    %w(lists locations).each do |collection_name|
      dump_last_n_docs(:areas, collection_name, 20_000)
    end
  end

  #----------
  protected

  def self.dump_entire_db(db_sym)
    cmd_str = dump_cmd_str(db_sym)
    puts cmd_str
    system(cmd_str)
  end

  # :: (sym, str, str) -> str
  def self.dump_cmd_str(db_sym, collection_name=nil, query_doc=nil)
    db_data = DBS[db_sym]
    db_name = db_data[:remote_name]
    srv = db_data[:server]
    host, port, username, password = srv[:host], srv[:port], srv[:username], srv[:password]
    cmd_str = "mongodump --host #{host} --port #{port} --username #{username} --password #{password} --db #{db_name}"
    coll_str = collection_name ? "--collection #{collection_name}" : ''
    query_str = query_doc ? "--query '#{query_doc}'" : ''
    [cmd_str, coll_str, query_str].join(' ')
  end

  #----------------
  
  def self.dump_last_n_docs(db_sym, collection_name, n)
    collection = get_mongo_collection(db_sym, collection_name)
    doc_id = get_id_for_nth_most_recent_doc(collection, n)
    puts doc_id.inspect
    dump_docs_after_id(db_sym, collection_name, doc_id)
  end
  
  # :: (Mongo::Collection, int) -> BSON::ObjectId
  def self.get_id_for_nth_most_recent_doc(collection, n)
    cnt = collection.count()
    num_to_skip = (n > cnt) ? 0 : cnt - n
    cursor = collection.find({}, {limit: 1}).sort(:_id).skip(num_to_skip)
    cursor.next()['_id']
  end
  
  def self.dump_docs_after_id(db_sym, collection_name, doc_id)
    query_doc = "{_id : {$gte: ObjectId(\"#{doc_id.to_s}\")} }"
    cmd_str = dump_cmd_str(db_sym, collection_name, query_doc)
    puts cmd_str
    system(cmd_str)
  end
  
  #----------------
  
  # :: sym -> Mongo::DB
  def self.get_mongo_db(db_sym)
    db_name = DBS[db_sym][:remote_name]
    srv = SERVERS[db_sym]
    host, port, username, password = srv[:host], srv[:port], srv[:username], srv[:password]
    uri = "mongodb://#{username}:#{password}@#{host}:#{port}/#{db_name}"
    mg_client = MongoClient.from_uri(uri)
    db = mg_client.db(db_name)
  end
  
  def self.get_mongo_collection(db_sym, collection_name)
    get_mongo_db(db_sym)[collection_name]
  end
  
end

#-------------------

module Restorer

  def self.restore_db_to_local(db_sym)
    remote_name = DBS[db_sym][:remote_name]
    local_name = DBS[db_sym][:local_name]
    cmd_str = "mongorestore --drop -d #{local_name} --noOptionsRestore dump/#{remote_name}/"
    puts cmd_str
    system(cmd_str)
  end
  
  def self.restore_to_local()
    DBS.keys.each do |k|
      restore_db_to_local(k)
    end
  end
end

#-------------------
#-------------------

#-- MAIN --

Dumper::dump_remote()
Restorer::restore_to_local()


# def get_biz_cat(id_str)
#   coll = get_mongo_collection(:web, 'business_categories')
#   coll.find(id: id_str)
# end

# biz_cat = get_biz_cat('4fb4706abcd7ac45cf000013').next()
# puts biz_cat.inspect
