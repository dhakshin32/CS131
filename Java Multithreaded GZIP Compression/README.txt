Program Structure:

I had a total of 3 classes. Pigzj class was my main class that read all the blocks and feed them into a  thread pool of threads created from my second class SingleThreadedCompressor. My third class, Writer, was responsible for outputting the compressed blocks sequentially as they were added to the Concurrent hash map.

Within the first class I used an executor service to generate a thread pool based on the number input/number of processors available. Then a while loop would read a file from stdin and pass it to a new thread for compression. 

Within the Compressor class, a check is made in the dictionary concurrent hash map to see if there exists a dictionary. If so the compressor is set to use this and if not the no compressor is used. Next the block of data is compressed and then it is appended to another concurrent hash map that is used by the Writer thread to print the compressed data. It is appended with the block number which is used by the writer thread to ensure that data is outputted in the correct order.

Within the writer class I have a while loop that continues to output data sequentially from the concurrent hash map. In the event that a data block is not ready for output, I call wait() so that the thread waits for the data block. In another spot I call notififyAll() to wake the writer thread and alert it that the data block has been appended to the concurrent hash map and is ready for output.

I used a concurrent hash map since it was the most thread safe.

Some issues that I ran into while doing this project was figuring out how to alert a thread if is on the last block. I went through different iterations of this and ended up taking out the part of the code that calls compressor.finish() and making it its own function. Now, whenever the number of bytes read is -1 indicating that we are done, I break out of the while loop, wait for the writer to finish and then call this function. I realized that the compressor finish output did not depending on anything so I was able to pull it out of the compressor thread.

Performance Analysis:

Program	Processors	File Size	Output Size	Real Time(s)	User Time (s)	System Time(s)	Compression Ratio
Gzip	1		125.94		43.26		0m8.022s		0m7.565s		0m0.059s		2.911234397
Pigz	4		125.94		43.13		0m2.303s		0m7.462s		0m0.037s		2.920009274
Pigzj	4		125.94		43.13		0m2.683s		0m7.780s		0m0.397s		2.920009274
							
Gzip	1		125.94		43.26		0m7.804s		0m7.532s		0m0.071s		2.911234397
Pigz	3		125.94		43.13		0m2.802s		0m7.425s		0m0.092s		2.920009274
Pigzj	3		125.94		43.14		0m4.763s		0m7.723s		0m1.160s		2.919332406
							
Gzip	1		125.94		43.26		0m8.040s		0m7.555s		0m0.071s		2.911234397
Pigz	2		125.94		43.13		0m4.119s		0m7.441s		0m0.090s		2.920009274
Pigzj	2		125.94		43.14		0m4.755s		0m7.739s		0m0.508s		2.919332406
							
Gzip	1		125.94		43.26		0m8.039s		0m7.566s		0m0.056s		2.911234397
Pigz	1		125.94		43.13		0m8.167s		0m7.168s		0m0.304s		2.920009274
Pigzj	1		125.94		43.14		0m7.907s		0m7.767s		0m0.479s		2.919332406


This analysis shows us that as the number of processors decrease, the real time starts to increase. We know that gzip is single threaded so we slowly approach the time value of gzip as we decrease the number of processors. In all cases we see that Pigz and Pigzj is better than gzip, except in the case of single thread where Pigz and Gzip are similar. We see that the user time remains about the same for pretty much all tests. This is because we are using the same file and file size throughout. This makes sense because user time is the cumulative time, across all threads, spent running the program. System time is greater for Pigzj than Pigz and Gzip. Since we define system time as the amount of time spent in kernel mode within a process, the reasoning for this is that since Pigzj requires use of the JVM, and is a much higher level language than C, there would be more system calls and more time having to go into kernel mode for things such as garage collection and other processes/activities. The compression ratios are similar throughout the tests indicating that all three programs are about the same in terms of compression size efficiency. Overall, this tells us that we were able to write a program that does a good job of implementing the features of pigz, except in the aspect that we used Java, a high level language that leads to slightly decreased efficiency.

System Calls:

Pigzj:

% time     seconds  usecs/call     calls    errors syscall
------ ----------- ----------- --------- --------- ----------------
 98.92   13.512842        3235      4176       404 futex
  0.32    0.043489          31      1377           mprotect
  0.31    0.042827          18      2365           read
  0.25    0.033773          34       972           write
  0.10    0.013196          19       691           sched_yield
  0.03    0.003647          91        40           madvise
  0.02    0.003083           2      1214           lseek
  0.01    0.001912           7       265           mmap
  0.01    0.001634           7       224       105 openat
  0.01    0.000717           7        96        52 stat
  0.00    0.000624           5       122           rt_sigprocmask
  0.00    0.000468           3       125           close
  0.00    0.000409           3       121           fstat
  0.00    0.000366           8        42           sched_getaffinity
  0.00    0.000353          16        22           munmap
  0.00    0.000280           3        72         1 lstat
  0.00    0.000194          38         5           rt_sigreturn
  0.00    0.000095           3        26           rt_sigaction
  0.00    0.000092           4        19           clone
  0.00    0.000091           4        20           set_robust_list
  0.00    0.000074           3        19           gettid
  0.00    0.000073          18         4           sendto
  0.00    0.000064          10         6           getdents64
  0.00    0.000058           4        14           prctl
  0.00    0.000050           4        12           getsockname
  0.00    0.000046           5         8           socket
  0.00    0.000042           4        10           getpid
  0.00    0.000035           4         8           getsockopt
  0.00    0.000033           3         9           prlimit64
  0.00    0.000032          16         2           readlink
  0.00    0.000029           7         4         2 access
  0.00    0.000028           7         4           poll
  0.00    0.000022           5         4           recvfrom
  0.00    0.000022           5         4           fcntl
  0.00    0.000022          11         2           ftruncate
  0.00    0.000020           5         4         4 bind
  0.00    0.000020          10         2         2 statfs
  0.00    0.000019           4         4           ioctl
  0.00    0.000019           4         4         4 connect
  0.00    0.000019           4         4           setsockopt
  0.00    0.000018           4         4           fchdir
  0.00    0.000017           4         4           brk
  0.00    0.000017          17         1           unlink
  0.00    0.000013           6         2           uname
  0.00    0.000013           6         2           sysinfo
  0.00    0.000012           6         2           kill
  0.00    0.000012           3         4           geteuid
  0.00    0.000012           6         2           clock_getres
  0.00    0.000009           4         2           getcwd
  0.00    0.000008           4         2         1 arch_prctl
  0.00    0.000007           7         1         1 mkdir
  0.00    0.000005           5         1           execve
  0.00    0.000005           5         1           set_tid_address
  0.00    0.000004           4         1           getuid
------ ----------- ----------- --------- --------- ----------------
100.00   13.660971                 12151       576 total

In total we see that there were a total of 12151 system calls, with a majority of them being made to futex. Futex is a system call made for locking used in threads which makes sense because we use a lot of locking in our program for the concurrent hash maps.

Pigz:% time     seconds  usecs/call     calls    errors syscall
------ ----------- ----------- --------- --------- ----------------
 73.52    0.281215          47      5864       456 futex
 19.00    0.072663          74       974           read
  6.43    0.024597          25       963           write
  0.41    0.001571          34        46           mmap
  0.32    0.001229          36        34           mprotect
  0.18    0.000681          24        28           munmap
  0.12    0.000445          89         5           clone
  0.02    0.000060          12         5           madvise
  0.01    0.000033           5         6           set_robust_list
  0.00    0.000009           1         8           brk
  0.00    0.000000           0         7           close
  0.00    0.000000           0         7           fstat
  0.00    0.000000           0         5           lseek
  0.00    0.000000           0         3           rt_sigaction
  0.00    0.000000           0         1           rt_sigprocmask
  0.00    0.000000           0         2         2 ioctl
  0.00    0.000000           0         1         1 access
  0.00    0.000000           0         1           execve
  0.00    0.000000           0         2         1 arch_prctl
  0.00    0.000000           0         1           set_tid_address
  0.00    0.000000           0         7           openat
  0.00    0.000000           0         1           prlimit64
------ ----------- ----------- --------- --------- ----------------
100.00    0.382503                  7971       460 total

For Pigz, we see a total of 7971 system calls being made and again with a majority of them being made to futex. Overall there are less number of different system calls compared to Pigzj.

Gzip:

% time     seconds  usecs/call     calls    errors syscall
------ ----------- ----------- --------- --------- ----------------
 70.72    0.009537        2384         4           close
 24.94    0.003363          20       166           write
  4.10    0.000553           0      3846           read
  0.14    0.000019           1        12           rt_sigaction
  0.07    0.000009           3         3           fstat
  0.04    0.000005           5         1         1 ioctl
  0.00    0.000000           0         1           lseek
  0.00    0.000000           0         5           mmap
  0.00    0.000000           0         4           mprotect
  0.00    0.000000           0         1           munmap
  0.00    0.000000           0         1           brk
  0.00    0.000000           0         1         1 access
  0.00    0.000000           0         1           execve
  0.00    0.000000           0         2         1 arch_prctl
  0.00    0.000000           0         2           openat
------ ----------- ----------- --------- --------- ----------------
100.00    0.013486                  4050         3 total

For Gzip we see only 4050 system calls being made and we see even less variation in the types of system calls.

Overall, Pigzj had the most number of syscalls followed by Pigz and then Gzip. This makes sense due to the fact that Pigzj is coded in java and also it had a higher system time in the performance analysis section. We conclude that Pigzj spends the most time in kernel mode. The reason for this is because it is a multithreaded java program. Because it is multithreaded, it uses more system calls in general when compared to Gzip and because its coded in Java and uses a JVM that has other processes going on behind the scenes such as garbage collection and automatic memory management, it has more system calls that Pigz.

