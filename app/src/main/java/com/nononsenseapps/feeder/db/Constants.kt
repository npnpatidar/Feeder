package com.nononsenseapps.feeder.db

import java.time.Instant

const val FEEDS_TABLE_NAME = "feeds"
const val FEED_ITEMS_TABLE_NAME = "feed_items"
const val SYNC_REMOTE_TABLE_NAME = "sync_remote"
const val SYNC_DEVICE_TABLE_NAME = "sync_device"

const val COL_ID = "id"
const val COL_TITLE = "title"
const val COL_CUSTOM_TITLE = "custom_title"
const val COL_URL = "url"
const val COL_TAG = "tag"
const val COL_NOTIFY = "notify"
const val COL_GUID = "guid"
const val COL_PLAINTITLE = "plain_title"
const val COL_PLAINSNIPPET = "plain_snippet"
const val COL_IMAGEURL = "image_url"
const val COL_IMAGE_FROM_BODY = "image_from_body"
const val COL_ENCLOSURELINK = "enclosure_link"
const val COL_ENCLOSURE_TYPE = "enclosure_type"
const val COL_LINK = "link"
const val COL_AUTHOR = "author"
const val COL_PUBDATE = "pub_date"
const val COL_NOTIFIED = "notified"
const val COL_FEEDID = "feed_id"
const val COL_FEEDTITLE = "feed_title"
const val COL_FEEDCUSTOMTITLE = "feed_customtitle"
const val COL_FEEDURL = "feed_url"
const val COL_LASTSYNC = "last_sync"
const val COL_RESPONSEHASH = "response_hash"
const val COL_FIRSTSYNCEDTIME = "first_synced_time"
const val COL_PRIMARYSORTTIME = "primary_sort_time"
const val COL_FULLTEXT_BY_DEFAULT = "fulltext_by_default"
const val COL_FETCH_SUMMARY_BY_DEFAULT = "fetch_summary_by_default"
const val COL_OPEN_ARTICLES_WITH = "open_articles_with"
const val COL_ALTERNATE_ID = "alternate_id"
const val COL_CURRENTLY_SYNCING = "currently_syncing"
const val COL_LATEST_MESSAGE_TIMESTAMP = "latest_message_timestamp"
const val COL_SYNC_CHAIN_ID = "sync_chain_id"
const val COL_DEVICE_ID = "device_id"
const val COL_DEVICE_NAME = "device_name"
const val COL_SECRET_KEY = "secret_key"
const val COL_WHEN_MODIFIED = "when_modified"
const val COL_LAST_FEEDS_REMOTE_HASH = "last_feeds_remote_hash"
const val COL_BOOKMARKED = "bookmarked"
const val COL_GLOB_PATTERN = "glob_pattern"
const val COL_FULLTEXT_DOWNLOADED = "fulltext_downloaded"
const val COL_READ_TIME = "read_time"
const val COL_SITE_FETCHED = "site_fetched"
const val COL_WORD_COUNT = "word_count"
const val COL_WORD_COUNT_FULL = "word_count_full"
const val COL_SKIP_DUPLICATES = "skip_duplicates"
const val COL_BLOCK_TIME = "block_time"
const val COL_RETRY_AFTER = "retry_after"

// year 5000
val FAR_FUTURE = Instant.ofEpochSecond(95635369646)
