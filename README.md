# Windowed word count

This application reads the lines emitted by a process as JSON lines with attributes `event_type`, `data`, and `timestamp`, and performs a windowed  word count, grouped by `event_type`.

## Contribute

This project has 3 main processes:
* Socket server
* Blackbox launcher
* Http server
#### Socket server
This is the stream process that do the word count of events (JSON messages from the `blackbox` application).  
Basically it is continually reading messages from a socket port, and, based on a window setup, doing the word count by `event_type` to be stored in a table of a local H2 database.  
By default, it is listening on `localhost:1234`

#### Blackbox launcher
This is the starting point of the source application (`blackbox.amd64`)  
An external component is required (`nc`) to perform the redirection from the console to a port in a target host machine.
```
./blackbox.amd64 | nc locahost 1234
```
Netcat can be installed in [Windows](https://sourceforge.net/projects/nc110/), [MacOSx](http://macappstore.org/netcat/) or [others](https://zoomadmin.com/HowToInstall/UbuntuPackage/netcat) Linux based operating system.

#### Http server
This is a very basic server that is waiting for requests to expose with the current word count over an HTTP interface.  
By default, it is running on `http://localhost:8090`. It has 1 end point with prefix path `/events/count`, and 2 routes.  
* Root (`http://localhost:8090/events/count/`), which return a json array of an event type count representation.  
  * Example: `[{"eventType":"bar","count":4},{"eventType":"baz","count":2},{"eventType":"foo","count":2}]`
  
* An even type (`http://localhost:8090/events/count/bar`), which return a json object of the event type count representation.  
  * Example: `{"eventType":"bar","count":4}`

---
### Build

To Package this project use sbt as follows:
```
sbt assembly
```
It will generate a jar file under _root_project_folder_/target/scala-2.13/zio-tech-challenge-assembly-1.0.0.jar

---

### Testing

To run unit tests with sbt:
```
sbt test
```

## Run it
* Run (stream process, api server and blackbox application)  
By default the tree processes will be executed with the default values.  
  
|Process|location | Detail|  
|----|-----|-------|  
|stream|localhost:1234|watermark of 5 seconds|
|api|localhost:8090||
|blackbox|/your/path/to/blackbox.amd64 &#124; nc localhost 1234| You need to update the application.conf file to point to your binary blackbox in your machine
```
sbt run
```
---
* Run combination with the default values.  
For the imaginary case when the binary is running in another machine.   
  You can launch any combination of processes with the `mode` command as follows: 
```
sbt "runMain org.irach.challenge.WordCountApp mode -ss true -api true -bb false"
```
|Process|Launched | 
|----|-----|  
|stream|Yes (-ss true) |
|api| Yes(-api true)|
|blackbox|No (-bb false)|

---
## Window Strategy  
It is possible to use different window strategies for the count of events.  
The following option allows you to change the strategy count. 
```
-v, --socket-server-window-strategy <value>
Window strategy to use [`watermark` | event-count | event-time]
```
Each option has its configurations associated
###watermark
```
-v watermark --socket-server-watermark-interval 5000 <Interval of processing time in milliseconds>
```
For this example the window will finish every 5 seconds processing time.
###event-count
```
-v event-count --socket-server-event-count-limit 100 <Number of events by window>
```
For this example the window will finish every 100 events received, regardless of the processing time.
###event-time
```
-v event-time --socket-server-event-time-limit 5000 <Interval of event time in milliseconds>
```
For this example the window will finish every 5 seconds event time (the timestamp field of the messages).

####The following are all the options available to count the word from the blackbox binary: 

``` 
BlackBox Word Count 1.0
Usage: blackbox word count [mode] [options]

  -s, --socket-server-host <value>
                           Machine name for the socket server to run
  -t, --socket-server-port <value>
                           Machine port for the socket server to run
  -u, --socket-server-n-conn <value>
                           Number of connections
  -v, --socket-server-window-strategy <value>
                           Window strategy to use [`watermark` | event-count | event-time]
  -w, --socket-server-watermark-interval <value>
                           Interval time in milliseconds
  -x, --socket-server-event-count-limit <value>
                           Limit count for events by window
  -y, --socket-server-event-time-limit <value>
                           Event time size in milliseconds by window
  -a, --api-server-host <value>
                           Machine name for the api server to run
  -b, --api-server-port <value>
                           Machine port for the api  server to run
  -n, --blackbox-path <value>
                           Full path of the binary application [blackbox.amd64]
  -o, --blackbox-netcat-cmd <value>
                           The netcat command to use
  --help                   prints this usage text
All the options are optionals

Command: mode [options]
You can run in 3 modes [socket-server | blackbox | apî-server].
  -ss, --socket-server <value>
                           only run socket server streaming
  -api, --api-server <value>
                           only run api server
  -bb, --blackbox <value>  only run blackbox application

Process finished with exit code 137 (interrupted by signal 9: SIGKILL)
```