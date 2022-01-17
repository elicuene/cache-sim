# cache-sim
This was created as a project for my computer architecture class (CS250 @ Duke University)

Coded in April 2021, pushed to new GitHub repository in Jan. 2022.

Simulator takes in params that determine cache specifications such as the file for instructions, associativity, block size, cache size, write-back or write-allocate,
writethrough or write-no-allocate.

The replacement policy is always Least Recently Used

For example, “./cachesim tracefile 1024 4 wt 32” should simulate a cache that is 1024KB (=1MB), 
4-way set-associative, write-through/write-no-allocate, with 32B blocks

The trace file will be in the following format. Each line will specify a single load or store, the 16-bit address that is being accessed (in base-16), the size of the access in bytes, and the value to be written if the access is a store (in base-16).

Example Instructions:

store 25bb 2 c77e

load d531 4

For each instruction the simulator outputs the type of instruction (load/store), the address, and whether it is a hit or miss


The Testing files were provided by the instructors of the class, but all the code in cachesim.java was written by yours truly.
