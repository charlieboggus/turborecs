# Turborecs

A full-stack media recommendation application that combines personal logging with AI-powered taste profiling. Think Letterboxd meets Goodreads, but with intelligent recommendations based on your actual viewing and reading preferences.

## What Makes Turborecs Different

Instead of relying on simple genre matching or collaborative filtering, Turborecs builds a sophisticated "Taste Profile" by:

1. **AI-Powered Tagging**: Each piece of media you log is analyzed using Claude (Anthropic's LLM) to generate nuanced tags across four dimensions:
   - **Theme**: Core thematic elements (e.g., "obsessive ambition", "identity crisis", "corruption of power")
   - **Mood**: Emotional atmosphere (e.g., "dread", "whimsical", "melancholic")
   - **Tone**: Stylistic qualities (e.g., "dark", "satirical", "epic", "absurdist")
   - **Setting**: Time/place/world (e.g., "1970s Los Angeles", "deep space", "medieval fantasy")

2. **Weighted Taste Profiling**: Your ratings matter. Higher-rated media carries more weight in your taste profile, so the system learns what you *actually* love, not just what you've consumed.

3. **Contextual Recommendations**: The LLM generates recommendations based on the aggregate patterns in your taste profile, understanding subtle thematic connections that traditional recommendation systems miss.

## Tech Stack

**Backend**
- Kotlin + Spring Boot REST API
- PostgreSQL 16 database
- Claude AI (Haiku) via Anthropic API

**Frontend**
- Next.js (React)
- Server Actions & Server Components
- shadcn/ui + Tailwind CSS

**Infrastructure**
- Docker + Docker Compose
- Designed for NGINX reverse proxy deployment

**External APIs**
- The Movie Database (TMDB) for film/TV metadata
- Open Library for book metadata
- Anthropic Claude API for AI tagging and recommendations

## Features

### Current
- 📚 Log movies and books with ratings
- 🤖 Automatic AI-powered tagging with weighted scores
- 🎯 AI-generated recommendations based on your unique taste profile
- 📊 Taste profile derived from weighted tag aggregation
- 📥 CSV import from Letterboxd (Goodreads support planned)

### In Development
This project is under active development. The core logging and AI tagging features are functional, but the recommendation engine and user experience are still being refined.

## Getting Started

### Prerequisites

- Docker and Docker Compose
- API keys for:
  - Anthropic Claude API
  - TMDB API
  - Open Library (no key required)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/charlieboggus/turborecs.git
   cd turborecs
   ```

2. **Set up environment variables**
   
   Create a `.env` file in the root directory:
   ```env
   # PostgreSQL
   POSTGRES_DB=turborecs
   POSTGRES_USER=turborecs
   POSTGRES_PASSWORD=your_secure_password

   # API Keys
   ANTHROPIC_API_KEY=your_claude_api_key
   TMDB_API_KEY=your_tmdb_api_key

   # Internal Auth (generate random secure tokens)
   INTERNAL_AUTH_TOKEN=your_internal_token
   ADMIN_AUTH_TOKEN=your_admin_token
   ```

3. **Run with Docker Compose**
   ```bash
   docker-compose up --build
   ```

4. **Access the application**
   
   The frontend will be available at `http://localhost:3000`

### Production Deployment

For production deployments, it's **strongly recommended** to:
- Place an NGINX reverse proxy in front of the web service
- Only expose port 3000 through NGINX
- Use proper SSL/TLS certificates
- Keep the API and database on the internal Docker network (not exposed to the internet)

The Docker Compose configuration is designed with this architecture in mind - only the web service exposes ports, while the API and database remain on the internal bridge network.

## How It Works

### The Tagging Process

1. When you log a media item, Turborecs fetches metadata from TMDB or Open Library
2. This metadata (title, description, etc.) is sent to Claude AI
3. Claude analyzes the content and returns weighted tags (0.0-1.0) across four categories
4. Tags are stored with their weights and model version for future analysis
5. Concurrent tagging requests are prevented using PostgreSQL advisory locks

### Building Your Taste Profile

- Each tag from your logged media is weighted by your rating
- Higher-rated items contribute more strongly to your profile
- The system aggregates these weighted tags to understand patterns in your preferences
- Tag weights are normalized (clamped between 0.0 and 1.0) for consistency

### Getting Recommendations

The recommendation engine uses your aggregated taste profile to query Claude for suggestions that match your unique combination of thematic preferences, moods, tones, and settings - going beyond simple genre or collaborative filtering.

## Architecture

```
┌─────────────┐
│   Next.js   │ :3000 (exposed)
│     Web     │
└──────┬──────┘
       │ http://api:8080
       ↓
┌─────────────┐
│  Spring Boot│ (internal)
│     API     │
└──────┬──────┘
       │
       ↓
┌─────────────┐
│  PostgreSQL │ (internal)
│     16      │
└─────────────┘

External: Claude API, TMDB, Open Library
```

## Contributing

Turborecs is under active development. Contributions, issues, and feature requests are welcome! 

If you fork this project or use these ideas, please provide appropriate attribution.

## License

This software is licensed under the GNU General Public License Version 3. Please see [LICENSE](/LICENSE) for more details.

## Acknowledgments

- Built with [Claude](https://www.anthropic.com/claude) by Anthropic
- Movie data from [The Movie Database (TMDB)](https://www.themoviedb.org/)
- Book data from [Open Library](https://openlibrary.org/)

---

**Note**: This project is designed for self-hosting. While the AI tagging features use the Claude API (which has associated costs), you maintain full control over your data and hosting environment.