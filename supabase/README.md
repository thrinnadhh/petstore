# 🐾 PawsNearMe Supabase Backend Architecture & Keep-Alive Orchestration

This directory contains the configurations and custom serverless **Supabase Edge Functions** designed to run alongside the PawsNearMe hybrid BaaS setup.

---

## ⚡ The Free-Tier Keep-Alive Edge Function (`keep-alive`)

Supabase pauses free-tier projects automatically if they receive zero API traffic for a duration of one week. To prevent this automatic shutdown, we have introduced a lightweight **Keep-Alive Serverless Edge Function** that executes a minimal database lookup against the `profiles` table to maintain continuous uptime.

### 📂 File Structure
*   `functions/keep-alive/index.ts`: A Deno-based Supabase Edge Function that leverages service-role keys to bypass RLS policies and query a single mock record safely.

---

## 🚀 How to Deploy the Edge Function

Ensure you have the [Supabase CLI](https://supabase.com/docs/guides/cli) installed locally, then execute:

```bash
# 1. Login to your Supabase CLI (if not already authenticated)
supabase login

# 2. Link your local project directory to your remote Supabase Project Ref
supabase link --project-ref <YOUR_PROJECT_REFERENCE_ID>

# 3. Deploy the keep-alive serverless function
supabase functions deploy keep-alive --no-verify-jwt
```

> [!IMPORTANT]
> The `--no-verify-jwt` flag is required because the scheduler (whether an external cron job or an automated workflow) needs to trigger the function without requiring client session tokens. Authorization is handled securely within the function sandbox.

---

## 📅 Automated Scheduling Options (Choose One)

To automate the execution of the keep-alive function every 3-4 days, select one of the robust, 100% free options below:

### Option A: Native PostgreSQL Scheduler (`pg_cron` + `pg_net`) [Highly Recommended]
Supabase supports direct database scheduling inside PostgreSQL using the `pg_cron` and `pg_net` extensions.

1. Connect to your database using the **SQL Editor** in the Supabase Dashboard.
2. Enable the extension and schedule the HTTP POST request by running the following SQL script:

```sql
-- 1. Enable HTTP and scheduling capabilities inside Postgres
create extension if not exists pg_net;

-- 2. Schedule a recurring keep-alive cron job (runs every 3 days at midnight)
select cron.schedule(
  'pawsnearme-keepalive-ping',
  '0 0 */3 * *',
  $$
  select net.http_post(
    url := 'https://<YOUR_PROJECT_REF>.supabase.co/functions/v1/keep-alive',
    headers := '{"Content-Type": "application/json", "Authorization": "Bearer <YOUR_ANON_KEY>"}'
  );
  $$
);
```

To list your scheduled cron jobs to verify it is running:
```sql
select * from cron.job;
```

---

### Option B: GitHub Actions Workflow
If you prefer managing automation inside your repository CI/CD pipeline, you can schedule a GitHub Actions workflow that pings the endpoint every three days.

Create `.github/workflows/keep-alive.yml` with the following configuration:

```yaml
name: 🐾 Supabase Keep-Alive Ping

on:
  schedule:
    # Runs at 00:00 every 3 days
    - cron: '0 0 */3 * *'
  workflow_dispatch: # Allows manual trigger from GitHub UI

jobs:
  ping:
    runs-on: ubuntu-latest
    steps:
      - name: Send keep-alive GET Request
        run: |
          curl -i -X GET "https://${{ secrets.SUPABASE_PROJECT_REF }}.supabase.co/functions/v1/keep-alive" \
            -H "Authorization: Bearer ${{ secrets.SUPABASE_ANON_KEY }}"
```

Add your `SUPABASE_PROJECT_REF` and `SUPABASE_ANON_KEY` as Repository Secrets in GitHub.

---

### Option C: External Cron Services (Easy Setup)
Alternatively, you can register the Edge Function URL (`https://<YOUR_PROJECT_REF>.supabase.co/functions/v1/keep-alive`) with a free cron scheduler such as:
*   [Cron-Job.org](https://cron-job.org)
*   [EasyCron](https://www.easycron.com)

Set the interval to run **every 3 days**, and supply a header parameter containing:
*   `Authorization`: `Bearer <YOUR_ANON_KEY>`
