# Troubleshooting Guide

## Common Issues and Solutions

This guide covers common problems you might encounter when running the H-Counter Bot and how to resolve them.

## Authentication Issues

### Problem: "Failed to obtain access token"

**Symptoms:**
- Bot fails to start
- Logs show authentication errors
- "Access token is missing or expired" messages

**Possible Causes:**
1. Incorrect Reddit credentials
2. Missing or invalid environment variables
3. Reddit API changes
4. Account suspended or restricted

**Solutions:**

1. **Verify Environment Variables:**
   ```bash
   # Check if variables are set
   echo $h_bot_id
   echo $h_bot_username
   # (Don't echo password/secret for security)
   ```

2. **Test Reddit Credentials:**
   - Try logging into Reddit manually with the same credentials
   - Check if the account is suspended or restricted

3. **Verify Reddit App Configuration:**
   - Go to https://www.reddit.com/prefs/apps
   - Ensure the app type is set to "script"
   - Verify client ID and secret are correct

4. **Check for Special Characters:**
   - Ensure passwords don't contain characters that need URL encoding
   - Try updating the password to use only alphanumeric characters

### Problem: "HTTP 401 Unauthorized"

**Solutions:**
1. Token may have expired - bot should automatically refresh
2. Check if Reddit account has necessary permissions
3. Verify the User-Agent string matches Reddit's requirements

## Rate Limiting Issues

### Problem: "HTTP 429 Too Many Requests"

**Symptoms:**
- Frequent 429 error responses
- Bot stops responding for extended periods
- High delay between responses

**Solutions:**

1. **Check Rate Limiting Logic:**
   - Verify the bot is reading rate limit headers correctly
   - Ensure proper delays are implemented

2. **Increase Base Delays:**
   ```java
   // In HttpUtil.java, increase the base delay
   Thread.sleep(5 * 1000); // Increase from 2 to 5 seconds
   ```

3. **Monitor Rate Limit Headers:**
   - Check logs for `X-Ratelimit-Remaining` values
   - Adjust logic if needed

### Problem: Bot is too slow to respond

**Solutions:**
1. **Reduce unnecessary API calls:**
   - Cache user data when possible
   - Optimize comment fetching logic

2. **Adjust delays for development:**
   ```java
   // Reduce delay for testing (not recommended for production)
   Thread.sleep(1000); // Reduce to 1 second
   ```

## Network and Connectivity Issues

### Problem: "Connection timeout" or "Network unreachable"

**Solutions:**

1. **Check Internet Connection:**
   ```bash
   ping reddit.com
   curl -I https://reddit.com
   ```

2. **Verify Firewall Settings:**
   - Ensure outbound HTTPS (443) is allowed
   - Check if proxy settings are needed

3. **DNS Issues:**
   ```bash
   nslookup reddit.com
   nslookup oauth.reddit.com
   ```

4. **Proxy Configuration:**
   ```java
   // Add to JVM arguments if behind proxy
   -Dhttp.proxyHost=proxy.company.com
   -Dhttp.proxyPort=8080
   -Dhttps.proxyHost=proxy.company.com
   -Dhttps.proxyPort=8080
   ```

## File and State Management Issues

### Problem: "Could not save processed message fullnames"

**Symptoms:**
- Bot processes same mentions repeatedly
- File permission errors in logs
- State file not being created or updated

**Solutions:**

1. **Check File Permissions:**
   ```bash
   # Linux/Mac
   ls -la /path/to/processed_ids.txt
   chmod 644 /path/to/processed_ids.txt
   chown botuser:botgroup /path/to/processed_ids.txt
   
   # Windows
   icacls "C:\path\to\processed_ids.txt" /grant botuser:F
   ```

2. **Verify Directory Exists:**
   ```bash
   # Create directory if it doesn't exist
   mkdir -p /path/to/directory
   ```

3. **Check Disk Space:**
   ```bash
   df -h  # Linux/Mac
   dir    # Windows
   ```

4. **File Locking Issues:**
   - Ensure no other processes are accessing the file
   - Check for antivirus interference

### Problem: Bot processes duplicate mentions

**Solutions:**
1. Check if state file is being written correctly
2. Verify the processed message fullnames are being loaded on startup
3. Ensure proper error handling in state management

## Memory and Performance Issues

### Problem: "OutOfMemoryError" or high memory usage

**Solutions:**

1. **Increase JVM Memory:**
   ```bash
   java -Xmx1g -Xms512m -jar bot.jar
   ```

2. **Optimize Comment Processing:**
   ```java
   // Limit comment fetching
   if (pages >= 5) { // Reduce from 10 to 5 pages
       hasMoreComments = false;
       break;
   }
   ```

3. **Monitor Memory Usage:**
   ```bash
   # Linux
   top -p $(pgrep java)
   
   # Windows
   tasklist /fi "imagename eq java.exe"
   ```

### Problem: Bot becomes unresponsive

**Solutions:**
1. **Add thread monitoring:**
   - Implement thread dumps for debugging
   - Add timeout handling

2. **Check for infinite loops:**
   - Review while loops in main update cycle
   - Add break conditions

3. **Restart mechanism:**
   - Implement automatic restart on unresponsive state
   - Use systemd or similar for auto-restart

## Reddit API Issues

### Problem: "Empty response body" or malformed JSON

**Solutions:**

1. **Add Response Validation:**
   ```java
   if (responseBody == null || responseBody.trim().isEmpty()) {
       System.err.println("Empty response received");
       return null;
   }
   ```

2. **Handle Partial Responses:**
   ```java
   try {
       JSONObject jsonResponse = new JSONObject(responseBody);
       // Process normally
   } catch (JSONException e) {
       System.err.println("Invalid JSON response: " + responseBody);
       return null;
   }
   ```

3. **Check Reddit Status:**
   - Visit https://www.redditstatus.com/
   - Check for API maintenance or outages

### Problem: User not found or no comments returned

**Solutions:**
1. **Handle Deleted/Suspended Users:**
   ```java
   if (comments.length == 0) {
       return "User " + targetUser + " has no accessible comments or doesn't exist.";
   }
   ```

2. **Check User Privacy Settings:**
   - Some users may have private profiles
   - Handle gracefully with appropriate messaging

## Bot Response Issues

### Problem: Bot posts malformed or empty responses

**Solutions:**

1. **Add Response Validation:**
   ```java
   if (result == null || result.trim().isEmpty()) {
       result = "Sorry, I couldn't analyze this user's comments.";
   }
   ```

2. **Handle Special Characters:**
   ```java
   // Ensure proper URL encoding
   String encodedReply = URLEncoder.encode(replyText, StandardCharsets.UTF_8);
   ```

3. **Check Comment Body Parsing:**
   ```java
   // Add null checks for comment bodies
   if (comment != null && !comment.trim().isEmpty()) {
       totalChars += comment.length();
       // ... rest of processing
   }
   ```

### Problem: Bot replies to wrong users or comments

**Solutions:**
1. **Verify Mention Parsing:**
   - Check target user extraction logic
   - Validate mention format parsing

2. **Add Debug Logging:**
   ```java
   System.out.println("Processing mention: ID=" + id + 
                     ", Author=" + author + 
                     ", Target=" + targetUser);
   ```

## Development and Debugging

### Enabling Debug Mode

1. **Add Debug Logging:**
   ```java
   private static final boolean DEBUG = true;
   
   if (DEBUG) {
       System.out.println("Debug: " + message);
   }
   ```

2. **Verbose HTTP Logging:**
   ```java
   // In HttpUtil.java, enable full response logging
   System.out.println("Full Response: " + responseBody);
   ```

3. **JVM Debug Options:**
   ```bash
   java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar bot.jar
   ```

### Testing Strategies

1. **Unit Testing Mention Parsing:**
   ```java
   // Test with sample mention data
   String testMention = "u/h-counter-bot u/testuser";
   // Verify parsing logic
   ```

2. **Mock Reddit Responses:**
   - Create sample JSON responses for testing
   - Test error handling with invalid responses

3. **Integration Testing:**
   - Test with a dedicated test subreddit
   - Use a separate test Reddit account

## Common Error Messages and Solutions

| Error Message | Cause | Solution |
|---------------|--------|----------|
| `JSONException: A JSONObject text must begin with '{'` | Invalid JSON response | Add response validation and error handling |
| `FileNotFoundException: processed_ids.txt` | State file doesn't exist | Create directory and file with proper permissions |
| `ConnectException: Connection refused` | Network/proxy issues | Check internet connection and proxy settings |
| `IllegalArgumentException: Invalid grant_type` | Wrong OAuth parameters | Verify Reddit app configuration |
| `NullPointerException in parseComments` | Unexpected API response format | Add null checks and defensive programming |

## Getting Help

If you're still experiencing issues:

1. **Check Logs Thoroughly:**
   - Look for stack traces and error patterns
   - Check timing of errors

2. **Reddit API Documentation:**
   - Visit https://www.reddit.com/dev/api/
   - Check for recent API changes

3. **Community Support:**
   - Post in r/redditdev for API questions
   - Visit r/hcounterbot for bot-specific issues

4. **Create GitHub Issues:**
   - Include full error logs
   - Provide steps to reproduce
   - Specify environment details

## Prevention and Best Practices

1. **Regular Monitoring:**
   - Set up log monitoring alerts
   - Monitor bot response times
   - Check state file updates

2. **Graceful Error Handling:**
   - Always handle exceptions appropriately
   - Provide fallback responses
   - Log errors with context

3. **Rate Limit Respect:**
   - Always respect Reddit's rate limits
   - Implement exponential backoff
   - Monitor API usage

4. **Security Practices:**
   - Never log sensitive credentials
   - Use environment variables for configuration
   - Regularly rotate Reddit passwords

5. **Testing:**
   - Test in isolated environment first
   - Validate with small datasets
   - Monitor behavior over time

---

This troubleshooting guide should help resolve most common issues with the H-Counter Bot. For additional support, check the project's GitHub repository or contact the maintainers.
