done 1. write javadocs for all classes explaining why and how to use; test classes contatinin lot of code restructure to use nested classes for example (BaseCaeses CornerCases Failures), and anything on your      
oppinion in each case   

2. for matcher create a wrappring coordinator with varying behavior
   e.g. stop one on first match / stop all on first match / exclude item on Nth failure / etc.
   if result is undefined can retry M times

3. The ConnectOutcome should be generified to MatchOutcome(FAILURE, MATCH, MISMATCH, ???anything else needed???)

4. investigate idea how to vary the behaviour 
and make left-right in-out-switch places of the loops, but preserve the behavior in ` do_match(l, r)`
because L and R are different types and cant be just swapped
```
left.forEach(L -> {
    right.forEach(R -> {
       do_match(L, R)
    })
}
```


5. Network class should calculate network accessibility based on the signal strength - and we'll just exclude certain from the count.

6. the matcher can additionally log-debug the current signal strength for examined network

7. check the real time of connection when password is correct - call the connect try with that timeout
+ try several times on different networks, collect statistics 
+ pass that timeout to the connect command to fail fast

8. additionally check the output when connecting to networks: (second priority but may be interesting)
A. without password Like PullmanGuest; 
B. without password but with auth page like "Tbilisi Loves You"
