`topic`  
The Kafka topic to write to.

- Type: string
- Importance: high

`channels.count`  
Number of channels

- Type: int
- Default: 1
- Importance: medium

`endian`  
Endianness of each sample. Permitted values are 'big' and 'little'

- Type: string
- Default: little
- Importance: medium

`sample.rate`  
Sample rate for the audio, in hertz

- Type: double
- Default: 44100.0
- Importance: medium

`sample.size`  
Size of each sample, in bits

- Type: int
- Default: 16
- Importance: medium

`signed`  
Whether samples are signed

- Type: boolean
- Default: true
- Importance: medium

`buffer.size`  
Size in bytes of the buffer used to transfer audio to/from the OS

- Type: int
- Default: 1024
- Valid Values: \[1,...\]
- Importance: low

`partition`  
The partition of the Kafka topic to write to

- Type: int
- Default: 0
- Importance: low
