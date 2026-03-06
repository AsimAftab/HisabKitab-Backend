# Deploying HisabKitab Backend to Render

## Prerequisites

- A [Render](https://render.com) account
- A PostgreSQL database (Render managed or external like Supabase)
- This repository pushed to GitHub

## Render Setup

1. **Create a New Web Service** on Render
2. **Connect your GitHub repository**
3. **Configure the service:**
   - **Environment**: Docker
   - **Region**: Choose the closest to your users
   - **Instance Type**: Free (or paid for production)
   - **Health Check Path**: `/actuator/health`

## Environment Variables

Set the following environment variables in the Render dashboard:

| Variable | Description | Example |
|---|---|---|
| `DATABASE_URL` | JDBC PostgreSQL connection URL | `jdbc:postgresql://host:5432/dbname` |
| `DATABASE_USERNAME` | Database username | `postgres` |
| `DATABASE_PASSWORD` | Database password | `your-db-password` |
| `JWT_SECRET` | Secret key for signing JWTs (min 256-bit) | `your-256-bit-secret` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `https://yourapp.com` |
| `APP_BASE_URL` | Full public URL of this service | `https://your-service.onrender.com` |

## Health Check

The app exposes `/actuator/health` which returns:

```json
{"status": "UP"}
```

Render uses this endpoint to verify the service is running. Configure it as the **Health Check Path** in your Render service settings.

Kubernetes-style probes are also available:
- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`

## Keep-Alive (Render Free Tier)

Render free tier spins down services after 15 minutes of inactivity. This app includes a built-in keep-alive scheduler (`KeepAliveScheduler`) that pings `/actuator/health` every 14 minutes when running with the `prod` profile.

This requires `APP_BASE_URL` to be set correctly.

### External Backup (Recommended)

For extra reliability, set up an external cron service to ping your health endpoint:

- [cron-job.org](https://cron-job.org) (free) — ping `https://your-service.onrender.com/actuator/health` every 14 minutes
- [UptimeRobot](https://uptimerobot.com) (free) — monitor the same URL with 5-minute intervals

## How It Works

- The `Dockerfile` uses a multi-stage build: JDK 17 for building, JRE 17 for runtime
- `SPRING_PROFILES_ACTIVE=prod` is set in the Docker image, loading `application-prod.properties`
- Flyway runs migrations on startup
- The app runs as a non-root user inside the container

## Troubleshooting

### Service fails to start
- Check that all environment variables are set correctly in Render
- Verify the database is accessible from Render (check connection URL, firewall rules)
- Check Render logs for stack traces

### Health check fails
- Ensure the health check path is set to `/actuator/health`
- Allow enough startup time (increase health check grace period to 60s+)

### Database connection errors
- Verify `DATABASE_URL` uses the JDBC format: `jdbc:postgresql://host:port/dbname`
- Check that `DATABASE_USERNAME` and `DATABASE_PASSWORD` are correct
- Ensure the database allows connections from Render IPs

### Keep-alive not working
- Verify `APP_BASE_URL` is set to the full public URL (including `https://`)
- Check logs for "Keep-alive ping" messages
- Confirm the `prod` profile is active
