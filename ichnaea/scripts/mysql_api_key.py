# Python Script to edit or create API Keys
import sys
import pymysql
import datetime
import struct
import binascii


# Display all API keys
def key_list(cursor):

	sql = "SELECT valid_key FROM api_key"
	cursor.execute(sql)
	result = cursor.fetchall()
	i = 1
	print("\nAPI Keys:")
	for key in result:
		print("%d) %s" % (i, key['valid_key']))
		i += 1


# Display all API Key info
def key_info(cursor):

    sql = "SELECT * FROM api_key"
    cursor.execute(sql)
    result = cursor.fetchall()
    i = 1
    print("\nAPI Key Info:\n")
    for key in result:
    	print("%d) %s\n\tvalid_key: %s\n\tallow_fallback: %s\n\tallow_locate: %s\n\tallow_region: %s\n\tfallback_name: %s\n\tfallback_schema: %s\n\tfallback_url: %s\n\tfallback_ratelimit: %s\n\tfallback_ratelimit_interval: %s\n\tfallback_cache_expire: %s\n\tstore_sample_locate: %s\n\tstore_sample_submit: %s\n" % (i, key['valid_key'], key['valid_key'], key['allow_fallback'], key['allow_locate'], key['allow_region'], key['fallback_name'], key['fallback_schema'], key['fallback_url'], key['fallback_ratelimit'], key['fallback_ratelimit_interval'], key['fallback_cache_expire'], key['store_sample_locate'], key['store_sample_submit']))
    	i += 1


# Create a new API Key
def key_create(cursor):

	fallback_name=fallback_schema=fallback_url=fallback_ratelimit=fallback_ratelimit_interval=fallback_cache_expire=None

	try:
		valid_key = sys.argv[2]
	except IndexError:
		valid_key = input("Enter new key: ")
	allow_locate = input("Enable location requests? 1 is yes, 0 is no: ")
	allow_region = input("Enable region requests? 1 is yes, 0 is no: ")
	allow_fallback = input("Enable fallbacks? 1 is yes, 0 is no: ")
	if allow_fallback == '1':
		fallback_name = input("Enter fallback name: ")
		fallback_schema = input("Enter fallback schema type: ")
		fallback_url = input("Enter fallback URL endpoint: ")
		fallback_ratelimit = input("Enter fallback rate limit (OPTIONAL, hit enter to skip): ")
		fallback_ratelimit_interval = input("Enter fallback rate limit interval (OPTIONAL, hit enter to skip): ")
		fallback_cache_expire = input("Enter fallback cache expire time (OPTIONAL, hit enter to skip): ")
		if fallback_ratelimit == "":
			fallback_ratelimit = 10
		if fallback_ratelimit_interval == "":
			fallback_ratelimit_interval = 60
		if fallback_cache_expire == "":
			fallback_cache_expire = 86400
	store_sample_locate = input("Enter number of locate samples to store (OPTIONAL, hit enter to skip): ")
	store_sample_submit = input("Enter number of submit samples to store (OPTIONAL, hit enter to skip): ")
	if store_sample_locate == "":
		store_sample_locate = 100
	if store_sample_submit == "":
		store_sample_submit = 100

	print("Creating key...")
	
	try:
		sql = "INSERT INTO api_key (valid_key, allow_fallback, allow_locate, allow_region, fallback_name, fallback_schema, fallback_url, fallback_ratelimit, fallback_ratelimit_interval, fallback_cache_expire, store_sample_locate, store_sample_submit) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)"
		cursor.execute(sql, (valid_key, allow_fallback, allow_locate, allow_region, fallback_name, fallback_schema, fallback_url, fallback_ratelimit, fallback_ratelimit_interval, fallback_cache_expire, store_sample_locate, store_sample_submit))
		print("Key '%s' creation success!" % valid_key)
	except:
		print("Key '%s' creation fail: %s" % (valid_key, sys.exc_info()[1].args[1]))


# Delete an API Key
def key_delete(cursor):
	sql = "DELETE FROM api_key WHERE valid_key=%s"
	cursor.execute(sql, (sys.argv[2],))
	print("Key '%s' deletion success!" % sys.argv[2])


# Update a field of an API Key
def key_update(cursor):
	try:
		sql = "UPDATE api_key SET %s='%s' WHERE valid_key='%s'" % (sys.argv[3], sys.argv[4], sys.argv[2])
		cursor.execute(sql, ())
		print("Key '%s' update success! Field '%s' has been changed to '%s'" % (sys.argv[2], sys.argv[3], sys.argv[4]))
	except:
		print("Key update fail: %s" % sys.exc_info()[1].args[1])


# Connect to the database
connection = pymysql.connect(host='location.bboxx.co.uk',
                             port=3306,
                             user='location',
                             password='location',
                             db='location',
                             charset='utf8mb4',
                             cursorclass=pymysql.cursors.DictCursor)

try:
    with connection.cursor() as cursor:
    	try:
	    	if sys.argv[1] == 'info':
	    		key_info(cursor)
	    	elif sys.argv[1] == 'list':
	    		key_list(cursor)
	    	elif sys.argv[1] == 'create':
	    		key_create(cursor)
	    		connection.commit()
	    	elif sys.argv[1] == 'delete':
	    		key_delete(cursor)
	    		connection.commit()
	    	elif sys.argv[1] == 'update':
	    		key_update(cursor)
	    		connection.commit()
	    	elif sys.argv[1] == 'help':
	    		print("apikey by Ben Withers - https://github.com/Manicben \
	    			\nPython Tool for editing BBOXX Ichnaea API keys\n \
	    			\nusage: ./apikey [OPTION]\n\n \
	    			\nOPTION\tCommand(s)\t\t\t\tDescription\n \
	    			\nlist\t./apikey list\t\t\t\tLists available API keys. \
	    			\n\ninfo\t./apikey info\t\t\t\tLists keys with details. \
	    			\n\t./apikey info [key]\t\t\tLists details of specified key. \
	    			\n\ncreate\t./apikey create\t\t\t\tInitiates the key creation dialog. \
	    			\n\t./apikey create [key]\t\t\tInitiates the key creation dialog for given key. \
	    			\n\ndelete\t./apikey delete\t\t\t\tDeletes specified key. \
	    			\n\nupdate\t./apikey update [key] [field] [value]\tUpdates specified key with specified value for given field.\n")
    	except IndexError:
    		print("No mode provided, quitting...")
finally:
    connection.close()
