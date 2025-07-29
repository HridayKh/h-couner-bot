# Deployment Guide

## Overview

This guide covers deploying and running the H-Counter Bot in various environments, from development setups to production servers.

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- Reddit account with API credentials
- Persistent file storage for state management

## Environment Setup

### Development Environment

1. **Install Java 21**:
   ```bash
   # Windows (using Chocolatey)
   choco install openjdk21
   
   # Mac (using Homebrew)
   brew install openjdk@21
   
   # Linux (Ubuntu/Debian)
   sudo apt update
   sudo apt install openjdk-21-jdk
   ```

2. **Install Maven**:
   ```bash
   # Windows (using Chocolatey)
   choco install maven
   
   # Mac (using Homebrew)
   brew install maven
   
   # Linux (Ubuntu/Debian)
   sudo apt install maven
   ```

3. **Verify Installation**:
   ```bash
   java --version
   mvn --version
   ```

### Reddit API Setup

1. **Create Reddit Account**: Create a dedicated account for the bot
2. **Create Reddit Application**:
   - Go to https://www.reddit.com/prefs/apps
   - Click "Create App" or "Create Another App"
   - Fill in the form:
     - **Name**: h-counter-bot
     - **App type**: script
     - **Description**: Bot that counts 'h' characters in user comments
     - **About URL**: (optional)
     - **Redirect URI**: http://localhost:8080 (required but not used)
   - Click "Create app"
   - Note the client ID (under the app name) and secret

## Environment Variables

Create environment variables for configuration:

### Windows

**Method 1: Command Prompt**
```cmd
set h_bot_id=your_client_id_here
set h_bot_secret=your_client_secret_here
set h_bot_username=your_bot_username
set h_bot_pass=your_bot_password
set h_bot_file=C:\bots\h-counter\processed_ids.txt
```

**Method 2: PowerShell**
```powershell
$env:h_bot_id="your_client_id_here"
$env:h_bot_secret="your_client_secret_here"
$env:h_bot_username="your_bot_username"
$env:h_bot_pass="your_bot_password"
$env:h_bot_file="C:\bots\h-counter\processed_ids.txt"
```

**Method 3: System Environment Variables**
1. Right-click "This PC" → Properties → Advanced System Settings
2. Click "Environment Variables"
3. Under "System variables", click "New"
4. Add each variable name and value

### Linux/Mac

**Method 1: Export (Temporary)**
```bash
export h_bot_id="your_client_id_here"
export h_bot_secret="your_client_secret_here"
export h_bot_username="your_bot_username"
export h_bot_pass="your_bot_password"
export h_bot_file="/opt/h-counter-bot/processed_ids.txt"
```

**Method 2: .bashrc/.zshrc (Persistent)**
```bash
# Add to ~/.bashrc or ~/.zshrc
echo 'export h_bot_id="your_client_id_here"' >> ~/.bashrc
echo 'export h_bot_secret="your_client_secret_here"' >> ~/.bashrc
echo 'export h_bot_username="your_bot_username"' >> ~/.bashrc
echo 'export h_bot_pass="your_bot_password"' >> ~/.bashrc
echo 'export h_bot_file="/opt/h-counter-bot/processed_ids.txt"' >> ~/.bashrc

# Reload configuration
source ~/.bashrc
```

**Method 3: Environment File**
Create `.env` file and source it:
```bash
# .env file
h_bot_id=your_client_id_here
h_bot_secret=your_client_secret_here
h_bot_username=your_bot_username
h_bot_pass=your_bot_password
h_bot_file=/opt/h-counter-bot/processed_ids.txt

# Source the file
set -a; source .env; set +a
```

## Building the Application

1. **Clean and Compile**:
   ```bash
   mvn clean compile
   ```

2. **Run Tests** (if available):
   ```bash
   mvn test
   ```

3. **Package JAR**:
   ```bash
   mvn package
   ```

4. **Create Executable JAR** (add to pom.xml):
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-shade-plugin</artifactId>
       <version>3.4.1</version>
       <executions>
           <execution>
               <phase>package</phase>
               <goals>
                   <goal>shade</goal>
               </goals>
               <configuration>
                   <transformers>
                       <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                           <mainClass>main.Main</mainClass>
                       </transformer>
                   </transformers>
               </configuration>
           </execution>
       </executions>
   </plugin>
   ```

## Running the Bot

### Development Mode

**Using Maven**:
```bash
mvn exec:java -Dexec.mainClass="main.Main"
```

**Using Java directly**:
```bash
java -cp target/classes:target/dependency/* main.Main
```

### Production Mode

**Using JAR file**:
```bash
java -jar target/h-counter-bot-0.0.1-SNAPSHOT-shaded.jar
```

**With custom JVM options**:
```bash
java -Xmx512m -Xms256m -jar target/h-counter-bot-0.0.1-SNAPSHOT-shaded.jar
```

## Production Deployment

### Linux Service (systemd)

1. **Create service user**:
   ```bash
   sudo useradd -r -s /bin/false hcounterbot
   ```

2. **Create directory structure**:
   ```bash
   sudo mkdir -p /opt/h-counter-bot/logs
   sudo mkdir -p /opt/h-counter-bot/data
   sudo chown -R hcounterbot:hcounterbot /opt/h-counter-bot
   ```

3. **Copy JAR file**:
   ```bash
   sudo cp target/h-counter-bot-0.0.1-SNAPSHOT-shaded.jar /opt/h-counter-bot/
   sudo chown hcounterbot:hcounterbot /opt/h-counter-bot/*.jar
   ```

4. **Create environment file**:
   ```bash
   sudo nano /opt/h-counter-bot/.env
   ```
   ```
   h_bot_id=your_client_id_here
   h_bot_secret=your_client_secret_here
   h_bot_username=your_bot_username
   h_bot_pass=your_bot_password
   h_bot_file=/opt/h-counter-bot/data/processed_ids.txt
   ```

5. **Create systemd service**:
   ```bash
   sudo nano /etc/systemd/system/h-counter-bot.service
   ```
   ```ini
   [Unit]
   Description=H-Counter Reddit Bot
   After=network.target
   Wants=network-online.target
   
   [Service]
   Type=simple
   User=hcounterbot
   Group=hcounterbot
   WorkingDirectory=/opt/h-counter-bot
   EnvironmentFile=/opt/h-counter-bot/.env
   ExecStart=/usr/bin/java -Xmx512m -Xms256m -jar h-counter-bot-0.0.1-SNAPSHOT-shaded.jar
   Restart=always
   RestartSec=30
   StandardOutput=journal
   StandardError=journal
   
   [Install]
   WantedBy=multi-user.target
   ```

6. **Start and enable service**:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable h-counter-bot
   sudo systemctl start h-counter-bot
   ```

7. **Check service status**:
   ```bash
   sudo systemctl status h-counter-bot
   sudo journalctl -u h-counter-bot -f
   ```

### Docker Deployment

1. **Create Dockerfile**:
   ```dockerfile
   FROM openjdk:21-jre-slim
   
   # Create app directory
   WORKDIR /app
   
   # Create non-root user
   RUN addgroup --system appgroup && adduser --system --group appuser
   
   # Copy JAR file
   COPY target/h-counter-bot-0.0.1-SNAPSHOT-shaded.jar app.jar
   
   # Create data directory
   RUN mkdir -p /app/data && chown -R appuser:appgroup /app
   
   # Switch to non-root user
   USER appuser
   
   # Expose health check port (optional)
   EXPOSE 8080
   
   # Run the application
   CMD ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
   ```

2. **Create docker-compose.yml**:
   ```yaml
   version: '3.8'
   
   services:
     h-counter-bot:
       build: .
       environment:
         - h_bot_id=${h_bot_id}
         - h_bot_secret=${h_bot_secret}
         - h_bot_username=${h_bot_username}
         - h_bot_pass=${h_bot_pass}
         - h_bot_file=/app/data/processed_ids.txt
       volumes:
         - ./data:/app/data
       restart: unless-stopped
       logging:
         driver: "json-file"
         options:
           max-size: "10m"
           max-file: "3"
   ```

3. **Build and run**:
   ```bash
   docker-compose up -d
   ```

4. **View logs**:
   ```bash
   docker-compose logs -f h-counter-bot
   ```

### Windows Service

1. **Install NSSM** (Non-Sucking Service Manager):
   ```bash
   choco install nssm
   ```

2. **Create service**:
   ```cmd
   nssm install H-Counter-Bot
   ```

3. **Configure service in NSSM GUI**:
   - **Path**: `C:\Program Files\Java\jdk-21\bin\java.exe`
   - **Startup directory**: `C:\bots\h-counter-bot`
   - **Arguments**: `-jar h-counter-bot-0.0.1-SNAPSHOT-shaded.jar`
   - **Environment**: Add all h_bot_* variables

4. **Start service**:
   ```cmd
   nssm start H-Counter-Bot
   ```

## Monitoring and Maintenance

### Log Monitoring

**View live logs (systemd)**:
```bash
sudo journalctl -u h-counter-bot -f
```

**View live logs (Docker)**:
```bash
docker-compose logs -f h-counter-bot
```

### Health Checks

The bot doesn't currently include health check endpoints, but you can monitor:

1. **Process status**: Check if the process is running
2. **Log analysis**: Look for error patterns in logs
3. **File modification**: Check if processed_ids.txt is being updated
4. **Reddit activity**: Monitor bot account for recent replies

### Maintenance Tasks

1. **Log rotation**: Configure logrotate for systemd logs
2. **Backup state file**: Regularly backup processed_ids.txt
3. **Monitor disk space**: Ensure sufficient space for logs and state
4. **Update dependencies**: Keep Java and dependencies updated

### Troubleshooting

**Common Issues**:

1. **Authentication failures**:
   - Check environment variables
   - Verify Reddit credentials
   - Check client ID/secret

2. **Rate limiting**:
   - Check logs for 429 responses
   - Verify rate limiting logic is working
   - Consider increasing delays

3. **Network issues**:
   - Check internet connectivity
   - Verify Reddit API accessibility
   - Check firewall settings

4. **File permissions**:
   - Ensure bot can read/write state file
   - Check directory permissions

**Debug Commands**:
```bash
# Check environment variables
printenv | grep h_bot

# Check Java version
java --version

# Check if bot is listening on network
netstat -tulpn | grep java

# Check recent system logs
journalctl -xe
```

## Security Considerations

1. **Environment Variables**: Never commit credentials to version control
2. **File Permissions**: Restrict access to state files and configuration
3. **User Privileges**: Run bot with minimal required privileges
4. **Network Security**: Consider running behind a firewall
5. **Regular Updates**: Keep Java and dependencies updated
6. **Backup Strategy**: Regularly backup state and configuration

## Scaling Considerations

For high-volume deployments:

1. **Multiple Instances**: Run multiple bot instances with different credentials
2. **Load Balancing**: Distribute mentions across instances
3. **Database Storage**: Replace file-based state with database
4. **Message Queuing**: Use queues for processing mentions
5. **Caching**: Cache user comment data to reduce API calls

---

This deployment guide provides comprehensive instructions for running the H-Counter Bot in various environments, from development to production. Choose the deployment method that best fits your infrastructure and requirements.
