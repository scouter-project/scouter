## End user performance monitoring (Beta)

### Key features


#### Gathering script errors occurring on real user's browser.
Somewhere, somebody may feel **bad user's experience** against your web service now.  
How can we recognize, gather and analyse those?  
Now new feature of Scouter give us insight for our service's user experience.   

- We can see
    - error page url
    - error stack trace
    - User's OS, Machine, Browser...


#### Gethering browser's page navigation timing information 
Server side timing does not include network time between user and server but also browser's rendering time.  
It's __real user experience timing__ which depens on user's machine, location, ISP...   
And so we need navagation timing information that measured on user side.  

- We can get these information below by this feature.
    - access url
    - gxid from response(it may user connect with server side timing information.)
    - TCP connection time
    - DNS lookup time
    - Request time
    - Wait for response time
    - Response time(network duration - from response 1st byte to last byte)
    - Loading event time
    - Another resource getting time(images..)
    - User's IP(We can get geo location using it)
    - ...

#### Gathering Ajax timing
Gether timing information for service request by XMLHTTPRequest. 

- Gathering information
  - call url
  - response time measured on client side.
  - gxid from response(it may user connect with server side timing information.)
    

### How to use
 - Just add one line script on your page.
 
