# Ichnaea
The Ichnaea live instance used by BBOXX is a production version of Mozilla's [Ichnaea project](https://github.com/mozilla/ichnaea) (ver 2.2.0). The Ichnaea documentation is very detailed and it is recommended to read it. It contains information on the [architecture](https://mozilla.github.io/ichnaea/install/architecture.html) of the Ichnaea server code, as well as [installation](https://mozilla.github.io/ichnaea/install/index.html) ([development](https://mozilla.github.io/ichnaea/install/devel.html) or [production](https://mozilla.github.io/ichnaea/install/deploy.html)) and [debugging](https://mozilla.github.io/ichnaea/install/debug.html) instructions. Find the full docs [here](https://mozilla.github.io/ichnaea/).

If you are simply looking to use the Geolocation API, then the details are [here](https://mozilla.github.io/ichnaea/api/geolocate.html). Please note that the BBOXX Ichnaea live instance uses a different URL endpoint.

## API
The BBOXX Ichnaea instance is hosted at http://location.bboxx.co.uk. To make a geolocation request, make a HTTP POST request to http://location.bboxx.co.uk/v1/geolocate?key=API_KEY where API_KEY is the API token that you have been given. For testing purposes, use the "test" API key (http://location.bboxx.co.uk/v1/geolocate?key=test). The payload of the POST should be a JSON file following the [structure](https://mozilla.github.io/ichnaea/api/geolocate.html) defined in the Ichnaea docs.

Here's an example using cURL and a JSON file named `sample_ichnaea.json` in the current working directory
```
curl -d @sample_ichnaea.json -H 'Content-Type: application/json' -i http://location.bboxx.co.uk/v1/geolocate?key=test

```
Depending on the API key used, different fallback methods are used. Some keys will cause requests to be passed through to UnWired Labs in the case where Ichnaea is unable to estimate a location, typically due to insufficient cell tower data in the MySQL database. To test this behaviour, use the "test-unwired" API key, which uses the UnWired Labs fallback. The "test-mls" API key uses the Mozilla Location Services as a fallback (which should not be very different to BBOXX Ichnaea). However, the "test" API key has no fallback by default. lacf and ipf fallbacks are disabled on BBOXX Ichnaea as they are very inaccurate and the UnWired Labs fallback covers those cases.

## Server Features
The BBOXX Ichnaea live server instance has support for UnWired Labs request fallback. Otherwise it has the basic set of features of Ichnaea 2.2.0. This means that Sentry, StatsD, datamaps, public data export, etc are all disabled.

Another feature is the automatic daily updating of the database to the latest Mozilla Location Services (MLS) [full cell export](https://location.services.mozilla.com/downloads). A bash script downloads the lastest compressed cell export file, decompresses it and proceeds to run a Python script that uploads the data to the MySQL database. The MySQL syntax ensures that data is only either added or updated, records are never deleted. This script ensures that the BBOXX Ichnaea database is up to date with MLS (not completely, due to the initial dataset).

For other features, read the Ichnaea docs.

## Maintenance
It is NOT recommended to attempt to upgrade the BBOXX Ichnaea to newer versions of Ichnaea. Upgrading could delete the entire database. If you are confident with Docker and MySQL, you could backup the database, upgrade Ichnaea, then restore the database (warning, it contains a LOT of data and will take a lot of time). Please take care when following the upgrade instructions [here](https://mozilla.github.io/ichnaea/install/deploy.html#upgrade), especially when performing Alembic upgrades.

To SSH into the BBOXX Ichnaea, you will require a .pem file to grant SSH access.
```
ssh -i path/to/ichnaea-server.pem ubuntu@location.bboxx.co.uk
```

The HOME directory (`/home/ubuntu`) contains folders and scripts.

`env.txt` contains the environment variables needed for running Ichnaea.

The `backup` folder contains a zipped copy of the old BBOXX Ichnaea instance `ichnaea.zip`, documented [here](https://github.com/BBOXX/ichnaea). Along with that is a full database backup `ichnaea_full_backup.sql.gz` from before the upgrade to 2.2.0. The folder also contains other items that were used in the installation of Ichnaea 2.2.0 and can be ignored.

The `test_scripts` folder contains various Python scripts used in uploading CSV data to the database. These are only needed for CSV upload, they rarely need to be used.

The `mls-update` folders contains the script and logs used for updating the database to the lastest MLS full cell export. `mysql_mls_gsm.py` is the Python script used and the logs folder contains 2 logs, one containing full debug output and the other containing start/end times. An update takes roughly an hour to 2 hours. Anything under 30 mins is a fail and usually is a result of low disk space. Check `~/mls-update/logs/mls-update-prog.log` for detailed progress information, though checking `~/mls-update/logs/ml-update.log` is usually sufficient.
The bash executable script is located in `/etc/cron.daily` and is named `MLS-pull`. This gets run daily at 11pm and this (along with the other `cron.daily` scripts) can be changed via `/etc/crontab` if necessary.

To start/stop/restart the server use (inside the home dir, `/home/ubuntu`):
```
sudo ./server start|stop|restart
```
This will start/stop/restart docker containers and MySQL and REDIS. Ignore warnings regarding REDIS, for some reason it's PID file is not created upon REDIS server start.

To debug the server, opening a shell inside a docker container helps to identify issues with the database or redis.
```
sudo ./server shell
```
From here follow steps from the [debugging section](https://mozilla.github.io/ichnaea/install/debug.html) of the docs. However, do NOT perform alembic downgrades/upgrades as this may DELETE database data.

To access the MySQL CLI:
```
mysql -uroot -plocation location
```
It is recommended to not change any of the data inside the database. `SELECT` statements are acceptable to view table data. The only table that may be edited is the `api_key` table, where API keys are managed. `INSERT` may be used to add a new API key, or `UPDATE` may be used to modify an existing key. It is recommended to have MySQL knowledge before attempting to use the database.

To access the REDIS CLI (from either the SSH'd server shell or from a shell within a docker container):
```
redis-cli -h location.bboxx.co.uk
```

### Database Backup/Restore

To backup the database, use the following command:
```
mysqldump -uroot -plocation location | gzip -9 > ichnaea_full_backup.sql.gz
```
To backup just the cell data (if upgrading Ichnaea as a just in case measure):
```
mysqldump -uroot -plocation location cell_gsm cell_wcdma cell_lte | gzip -9 > ichnaea_cell_backup.sql.gz
```
Both commands produce compressed SQL files that can be used to restore the MySQL database, or just the cell data.

Before restoring, decompress the SQL file with:
```
gunzip -k [backupfile.sql.gz]
```

To restore the database, use the following:
```
nohup mysql -uroot -plocation location < [backupfile.sql.gz]
```
This command will run the MySQL import regardless if you exit the SSH session. The import can take several hours, so this is recommended. You can check if the command is running with `htop`. Be careful when importing a `ichnaea_full_backup.sql.gz` file, as this may not be compatible with newer versions of Ichnaea, due to Alembic migrations.