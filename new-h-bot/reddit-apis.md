POST https://oauth.reddit.com/api/comment
- **Input:**
  - `thing_id` (String): The Fullname of the parent comment or link to reply to (e.g., `t1_...` or `t3_...`).
  - `text` (String): The markdown body of the comment reply.
  - `api_type` (String): Set to `json` for JSON response.
- **Output:**
  - Returns a JSON object containing the `things` array if successful, or `errors` array if failed. The success response includes details of the posted comment.
- **Usage in code:** `CommentReply.java` in `replyToComment` method.

POST https://oauth.reddit.com/api/read_message
- **Input:**
  - `id` (String): A comma-separated list of Fullnames of messages (e.g., `t4_...`, `t1_...`) to mark as read. (Note: Parameter name in request body is `id`, documentation in code handles this by joining list).
- **Output:**
  - Returns a filtered response (typically JSON). The code checks for 202 status code generally for success, but effectively ignores the body content for logic flow, assuming success on non-error.
- **Usage in code:** `CommentReply.java` in `markMessagesAsReadBatch` method.

GET https://oauth.reddit.com/user/{author}/comments/.json?limit={limit}&after={after}
- **Input:**
  - `author` (Path Parameter): The username of the redditor.
  - `limit` (Query Parameter): Number of comments to fetch (e.g., 100).
  - `after` (Query Parameter): The Fullname of an item to start fetching after (for pagination).
- **Output:**
  - JSON object containing `data` -> `children` (array of comments) and `after` (token for next page). Each child contains comment data including `body`.
- **Usage in code:** `GetRedditorComments.java` in `getComments` method.

GET https://oauth.reddit.com/message/unread
- **Input:**
  - None explicitly passed in the GET request url in the code, but standard Reddit API supports limits and markers.
- **Output:**
  - JSON object containing `data` -> `children` (array of unread messages). Each child contains message details like `id`, `name` (fullname), `author`, `body`, `was_comment`, `context`, `parent_id`.
- **Usage in code:** `MentionsManager.java` in `getUnreadMessagesAndFilterMentions` method.

GET https://oauth.reddit.com/api/info.json?id={id}
- **Input:**
  - `id` (Query Parameter): The Fullname of the item (e.g., comment or link, `t1_...` or `t3_...`) to retrieve info for.
- **Output:**
  - JSON object containing `data` -> `children`. The first child's `data` contains the item's info, specifically used to retrieve the `author`.
- **Usage in code:** `MentionsManager.java` in `getParentRedditor` method.

GET https://oauth.reddit.com/r/{subreddit}/comments/{postId}/.json
- **Input:**
  - `subreddit` (Path Parameter): The subreddit name.
  - `postId` (Path Parameter): The ID of the post (submission).
- **Output:**
  - JSON Array. The first element contains the listing for the submission itself. Used to extract the `author` (OP) of the post from `[0].data.children[0].data.author`.
- **Usage in code:** `MentionsManager.java` in `getOP` method.

POST https://oauth.reddit.com/api/v1/access_token
- **Input:**
  - `grant_type` (String): `password`
  - `username` (String): Reddit username.
  - `password` (String): Reddit password.
  - **Auth Header:** Basic Auth (Client ID and Client Secret).
- **Output:**
  - JSON object containing `access_token` and `expires_in`.
- **Usage in code:** `TokenAuth.java` in `getAccessToken` method.
