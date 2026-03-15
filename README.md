#To run simply run the Main.java file..

# MiniBankOS
The goal is to simulate multiple transactions like a bank. There should be be concurrency control on the processes to keep the data consistent. All the processes should be synchronized. And incase of system failure the data shouldn't be wiped out. 

## Banking Transaction System with Concurrency Control 
### Idea: Simulate: 
Multiple users transferring money simultaneously 
Prevent inconsistent balances

### OS Concepts: 
Critical section problem 
Process synchronization 
Reader-writer problem 
Atomic operations 
Logging + crash recovery

### Advanced Twist: 
Simulate a system crash and implement recovery.

### Directory structure
BankingOS/
│
├── src/
│   │
│   ├── kernel/
│   │   ├── TransactionManager.java
│   │   ├── LockManager.java
│   │   └── RecoveryManager.java
│   │   │
│   │   └── scheduler/
│   │        ├── Scheduler.java
│   │        ├── PriorityScheduler.java
│   │        ├── RoundRobinQueue.java
│   │        └── TransactionProcess.java
│   │
│   ├── system/
│   │   ├── Account.java
│   │   └── BankDatabase.java
│   │
│   ├── shell/
│   │   ├── Terminal.java
│   │   └── CommandParser.java
│   │
│   ├── logging/
│   │   └── Logger.java
│   │
│   └── Main.java
│
├── data/
│   └── transaction.log
│
└── README.md

### Each folder useCase: 
src/
kernel/
    Core OS mechanisms
    concurrency, locking, recovery

system/
    Shared resources (accounts database)

shell/
    Command-line interface

logging/
    transaction logging for crash recovery

#Troubleshooting:
It may happen that code may be correct, but it isn't updating. (in case of logical error)
May be since we are just compiling the Main.java.
FOR IT TO REFLECT THE UPDATION USE COMMAND:
`javac -d .. (Get-ChildItem -Recurse -Filter *.java).FullName`
The above code compiles all the files in the directory simultaneously



data/
    persistent logs
