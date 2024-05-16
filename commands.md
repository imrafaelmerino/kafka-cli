* First iteration:

start-producer $producer_name

stop-producer $producer_name

start-consumer $consumer_name --instances $instances

stop-consumer $consumer_name


* Generators specified

send-gen $channel_name \
$value_gen_name \
$key_gen_name \

* Explicit values specified:

send-val $channel_name \
$value \
$key



* Second iteration: Same without Registry


* Third iteration: Other useful commands 

ls producers: (returns a list of all producers and topic and status (started or not))
ls consumers: (returns a list of all consumers and topic and instances and status (started or not))
ls channels (return list of channels)
ls generators (list of generators)
stats (prints out statistics: number messages produced by producer and consumeed by consumer)
