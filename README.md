# H-Counter Bot

A Reddit bot that analyzes users' comments to count the frequency of the letter 'h' and provides entertaining statistics and ratings based on their "H-Score".

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [How It Works](#how-it-works)
- [Usage](#usage)
- [API Endpoints](#api-endpoints)
- [Rate Limiting](#rate-limiting)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)

## Overview

The H-Counter Bot is a Java-based Reddit bot that monitors username mentions and responds with detailed statistics about how frequently a user uses the letter 'h' in their comments. It calculates an "H-Score" (ratio of 'h' characters to non-'h' characters) and provides humorous ratings based on the score.

## Features

- **Automated Mention Detection**: Monitors Reddit for mentions of `u/h-counter-bot`
- **Comment Analysis**: Fetches up to 1000 comments per user (10 pages × 100 comments)
- **H-Score Calculation**: Calculates the ratio of 'h' characters to total characters
- **Entertaining Responses**: Provides humorous ratings based on H-Score ranges
- **Rate Limit Handling**: Respects Reddit's API rate limits with automatic throttling
- **Persistent State**: Tracks processed messages to avoid duplicate responses
- **Batch Operations**: Efficiently marks multiple messages as read in single requests
- **Error Handling**: Comprehensive error handling and logging

## How It Works

1. **Authentication**: Uses OAuth2 to authenticate with Reddit API
2. **Message Monitoring**: Continuously polls for unread messages and mentions
3. **Comment Fetching**: Retrieves the target user's comment history
4. **Analysis**: Counts total characters and 'h' characters in all comments
5. **Response Generation**: Creates a formatted response with statistics and rating
6. **Reply Posting**: Posts the analysis as a reply to the original mention

### H-Score Calculation

```
H-Score = (h_count / (total_characters - h_count)) × 100
```

### Rating System

- **< 1%**: "Fuck You. Seriously, where's the h? Did you even try?"
- **1-2%**: "Is that even an h? I can barely see h! Or do my eyes deceive me."
- **2-3%**: "Barely an h enthusiast. Get those fingers moving, pal."
- **3-6%**: "Solid h game, not gonna lie. Pretty average, though."
- **6-10%**: "Above average h usage! You're clearly a person of culture."
- **10-20%**: "Whoa, that's a lot of hs! Are you sexually attracted to it?"
- **20-50%**: "Ah yes, an h enthusiast, HhHhHhHh. Impressive."
- **50-100%**: "The h whisperer! You speak fluent h. We are not worthy."
- **100-200%**: "Legendary h status! You're almost an h-bot"
- **200%+**: "An h demigod! Your h count is off the charts, but still human....... probably."

## Usage

### Mentioning the Bot

Users can mention the bot in several ways:

1. **Self Analysis**: `u/h-counter-bot [self]` - Analyzes the commenter's own posts
2. **Specific User**: `u/h-counter-bot u/username` - Analyzes a specific user
3. **Original Poster**: `u/h-counter-bot op` - Analyzes the original post author

### Example Response

```
Hey u/PROMAN8625! You wanted to know about the hScore of u/PROMAN8625?

Well, here's the lowdown:
u/PROMAN8625 has dropped 100 comments, flexing a grand total of 4202 characters.
Within those, I meticulously counted a whopping `128` *h* or *H* characters!

That brings us to the moment of truth: the legendary H-Score (that's 'h's per every non-'h' character, for the uninitiated) is a solid ***3.1400***, which is a rating of Barely an h enthusiast. Get those fingers moving, pal.

This message was brought to you by the H-Counter Bot, report error or issues to the mods or r/hcounterbot
```

## Rate Limiting

The bot implements comprehensive rate limiting:

- **Base Delay**: 2-second delay between all API requests
- **Rate Limit Monitoring**: Tracks `X-Ratelimit-Remaining` headers
- **Automatic Throttling**: Sleeps when remaining requests < 5
- **429 Handling**: Automatic retry with exponential backoff
- **Page Limiting**: Maximum 10 pages of comments per user (1000 comments)

## Project Structure

```
src/main/java/main/
├── Main.java              # Main application loop and entry point
├── MentionsManager.java   # Handles mention detection and parsing
├── GetRedditorComments.java # Fetches and analyzes user comments
├── CommentReply.java      # Generates responses and posts replies
├── HttpUtil.java          # HTTP request handling and rate limiting
└── TokenAuth.java         # OAuth2 authentication with Reddit
```

### Class Descriptions

#### Main.java
- **Purpose**: Application entry point and main loop
- **Key Functions**:
  - Environment variable management
  - Token refresh logic
  - Message processing coordination
  - Persistent state management

#### MentionsManager.java
- **Purpose**: Handles incoming mentions and message filtering
- **Key Functions**:
  - `getUnreadMessagesAndFilterMentions()`: Fetches and filters mentions
  - `parseComments()`: Parses mention content to determine target users
  - `getOP()`: Identifies original poster from context URLs

#### GetRedditorComments.java
- **Purpose**: Fetches and analyzes user comment data
- **Key Functions**:
  - `getComments()`: Fetches up to 1000 user comments with pagination
  - `parseCommentH()`: Counts total characters and 'h' characters

#### CommentReply.java
- **Purpose**: Generates responses and handles Reddit interactions
- **Key Functions**:
  - `determineResult()`: Creates formatted response with H-Score analysis
  - `replyToComment()`: Posts reply to Reddit
  - `markMessagesAsReadBatch()`: Batch marks messages as read

#### HttpUtil.java
- **Purpose**: Centralized HTTP request handling
- **Key Functions**:
  - `performHttpRequest()`: Makes HTTP requests with rate limiting
  - Rate limit monitoring and automatic throttling
  - Request/response logging

#### TokenAuth.java
- **Purpose**: Handles Reddit OAuth2 authentication
- **Key Functions**:
  - `getAccessToken()`: Obtains OAuth2 access token using username/password

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- Follow Java naming conventions
- Add comprehensive error handling
- Include logging for debugging
- Respect Reddit's API rate limits
- Write clear, documented code

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Disclaimer

This bot is designed for entertainment purposes. Please use responsibly and in accordance with Reddit's Terms of Service and API guidelines.

## Support

For issues, questions, or feature requests:
- Create an issue on GitHub
- Contact u/PROMAN8625 on Reddit
- Visit r/hcounterbot

---

**Version**: 1.1  
**Author**: u/PROMAN8625  
**Last Updated**: July 2025
