# MiniBankOS
The goal is to simulate multiple transactions like a bank. There should be be concurrency control on the processes to keep the data consistent. All the processes should be synchronized. And incase of system failure the data shouldn't be wiped out. 

## Banking Transaction System with Concurrency Control 
### Idea: Simulate: 
Multiple users transferring money simultaneously Prevent inconsistent balances 
### OS Concepts: 
Critical section problem 
Process synchronization 
Reader-writer problem 
Atomic operations 
Logging + crash recovery
### Advanced Twist: 
Simulate a system crash and implement recovery.
