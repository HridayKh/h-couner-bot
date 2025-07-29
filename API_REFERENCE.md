# API Reference

## Overview

This document provides detailed information about the Reddit API endpoints used by the H-Counter Bot and the internal API structure.

## Reddit API Endpoints

### Authentication

#### POST /api/v1/access_token
Obtains OAuth2 access token for Reddit API access.

**Headers:**
- `Authorization: Basic {base64(client_id:client_secret)}`
- `User-Agent: script_in_java:h-counter-bot:v1.1 (by u/PROMAN8625)`
- `Content-Type: application/x-www-form-urlencoded`

**Body:**
```
grant_type=password&username={username}&password={password}
```

**Response:**
```json
{
  "access_token": "string",
  "token_type": "bearer",
  "expires_in": 3600,
  "scope": "string"
}
```

### Messages

#### GET /message/unread
Fetches unread messages and mentions.

**Headers:**
- `Authorization: Bearer {access_token}`
- `User-Agent: script_in_java:h-counter-bot:v1.1 (by u/PROMAN8625)`

**Response:**
```json
{
  "kind": "Listing",
  "data": {
    "after": "string|null",
    "dist": 3,
    "children": [
      {
        "kind": "t1|t4",
        "data": {
          "id": "string",
          "name": "string",
          "author": "string",
          "body": "string",
          "type": "username_mention|unknown",
          "was_comment": true,
          "context": "string",
          "created_utc": 1751790128.0
        }
      }
    ]
  }
}
```

#### POST /api/read_message
Marks messages as read.

**Headers:**
- `Authorization: Bearer {access_token}`
- `User-Agent: script_in_java:h-counter-bot:v1.1 (by u/PROMAN8625)`
- `Content-Type: application/x-www-form-urlencoded`

**Body:**
```
id=t1_abc123,t4_def456,t1_ghi789
```

### Comments

#### GET /user/{username}/comments/.json
Fetches user's comment history with pagination.

**Parameters:**
- `username`: Reddit username
- `limit`: Number of comments per page (max 100)
- `after`: Pagination token for next page

**Headers:**
- `Authorization: Bearer {access_token}`
- `User-Agent: script_in_java:h-counter-bot:v1.1 (by u/PROMAN8625)`

**Response:**
```json
{
  "kind": "Listing",
  "data": {
    "after": "string|null",
    "dist": 100,
    "children": [
      {
        "kind": "t1",
        "data": {
          "id": "string",
          "author": "string",
          "body": "string",
          "created_utc": 1751790128.0,
          "subreddit": "string"
        }
      }
    ]
  }
}
```

#### POST /api/comment
Posts a reply to a comment.

**Headers:**
- `Authorization: Bearer {access_token}`
- `User-Agent: script_in_java:h-counter-bot:v1.1 (by u/PROMAN8625)`
- `Content-Type: application/x-www-form-urlencoded`

**Body:**
```
api_type=json&text={reply_text}&thing_id={thing_id}
```

**Response:**
```json
{
  "json": {
    "errors": [],
    "data": {
      "things": [
        {
          "kind": "t1",
          "data": {
            "id": "string",
            "body": "string",
            "author": "string",
            "created_utc": 1751798373.0
          }
        }
      ]
    }
  }
}
```

### Post Information

#### GET /api/info.json
Gets information about a specific post or comment.

**Parameters:**
- `id`: Full Reddit ID (e.g., "t3_abc123" for posts, "t1_def456" for comments)

**Headers:**
- `Authorization: Bearer {access_token}`
- `User-Agent: script_in_java:h-counter-bot:v1.1 (by u/PROMAN8625)`

**Response:**
```json
{
  "kind": "Listing",
  "data": {
    "children": [
      {
        "kind": "t1|t3",
        "data": {
          "id": "string",
          "author": "string",
          "title": "string",
          "body": "string"
        }
      }
    ]
  }
}
```

#### GET /r/{subreddit}/comments/{post_id}/.json
Gets post and comment information for OP detection.

**Parameters:**
- `subreddit`: Subreddit name
- `post_id`: Post ID

**Headers:**
- `Authorization: Bearer {access_token}`
- `User-Agent: script_in_java:h-counter-bot:v1.1 (by u/PROMAN8625)`

**Response:**
```json
[
  {
    "kind": "Listing",
    "data": {
      "children": [
        {
          "kind": "t3",
          "data": {
            "id": "string",
            "author": "string",
            "title": "string",
            "selftext": "string"
          }
        }
      ]
    }
  }
]
```

## Rate Limiting

### Headers
Reddit includes rate limiting information in response headers:

- `X-Ratelimit-Remaining`: Number of requests remaining in current window
- `X-Ratelimit-Reset`: Seconds until rate limit resets
- `X-Ratelimit-Used`: Number of requests used in current window

### Rate Limit Handling
The bot implements the following rate limiting strategy:

1. **Preventive Throttling**: 2-second delay between all requests
2. **Header Monitoring**: Checks remaining requests after each response
3. **Automatic Pausing**: Sleeps when remaining < 5 requests
4. **429 Response Handling**: Automatic retry with full reset wait time

### Rate Limit Logic
```java
if (remainingRequests < 5 || responseCode == 429) {
    long sleepFor = (resetTimeSeconds * 1000) + 2000;
    Thread.sleep(sleepFor);
}
```

## Internal API Structure

### Main Classes and Methods

#### Main.java
```java
public class Main {
    // Environment variables
    public static final String CLIENT_ID;
    public static final String CLIENT_SECRET;
    public static final String REDDIT_USERNAME;
    public static final String REDDIT_PASSWORD;
    
    // State management
    public static String TOKEN;
    public static Set<String> processedMessageFullnames;
    
    // Main loop
    public void update_loop();
    private void loadProcessedMessageFullnames();
    private void saveProcessedMessageFullnames();
}
```

#### MentionsManager.java
```java
public class MentionsManager {
    public static List<String[]> getUnreadMessagesAndFilterMentions(
        List<String> fullnamesToMarkAsRead
    );
    
    private static List<String[]> parseComments(
        JSONArray children, 
        List<String> fullnamesToMarkAsRead
    );
    
    private static String getParentRedditor(String parentId);
    private static String getOP(String context);
}
```

#### GetRedditorComments.java
```java
public class GetRedditorComments {
    public static String[] getComments(String author);
    public static long[] parseCommentH(String[] comments);
}
```

#### CommentReply.java
```java
public class CommentReply {
    public static String determineResult(
        long[] inf, 
        int totalComments, 
        String author, 
        String targetUser
    );
    
    public static boolean replyToComment(String thingId, String replyText);
    
    public static boolean markMessagesAsReadBatch(List<String> fullnames);
}
```

#### HttpUtil.java
```java
public class HttpUtil {
    public static long nextRequestTime;
    
    public static String performHttpRequest(
        String method, 
        URL url, 
        String postBody, 
        boolean isAccessTokenRequest
    );
    
    private static void rateLimit(
        String remaining, 
        String reset, 
        String used, 
        int responseCode
    );
}
```

#### TokenAuth.java
```java
public class TokenAuth {
    public static String[] getAccessToken();
}
```

### Data Flow

1. **Initialization**: Load processed message IDs from file
2. **Authentication**: Get OAuth2 access token
3. **Message Fetching**: Get unread messages from Reddit
4. **Mention Filtering**: Filter for bot mentions and parse target users
5. **Comment Analysis**: Fetch target user's comments and calculate H-Score
6. **Response Generation**: Create formatted response with statistics
7. **Reply Posting**: Post reply to original mention
8. **State Management**: Mark messages as read and save processed IDs

### Error Handling

The bot includes comprehensive error handling:

- **Network Errors**: Retry logic with backoff
- **API Errors**: Error response parsing and logging
- **Rate Limiting**: Automatic throttling and respect for limits
- **Data Parsing**: Null checks and fallback values
- **File I/O**: Exception handling for state persistence
- **Authentication**: Token refresh on expiration

### Logging

The bot provides detailed logging:

- HTTP request/response details
- Rate limiting information
- Processing status for each mention
- Error messages with stack traces
- State management operations
