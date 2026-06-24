# SWRLZ-CORE Independent Mentor Backend

This backend is no longer tied to an Emergent preview URL or Emergent credits.
It stores missions, events, skills, and Council history in a local SQLite file.
OpenAI is optional and is used only for online Mentor planning/chat/distillation.

## Local start

```bash
cd backend
python -m venv .venv
. .venv/bin/activate              # Windows: .venv\\Scripts\\activate
pip install -r requirements.txt
cp .env.example .env
uvicorn server:app --host 0.0.0.0 --port 8000 --reload
```

Check `http://127.0.0.1:8000/api/health`.

## Environment

- `OPENAI_API_KEY`: optional; enables Mentor planning, Council chat, and skill distillation.
- `OPENAI_MODEL`: optional model identifier.
- `SWRLZ_API_TOKEN`: recommended for any non-local/private deployment.
- `SWRLZ_DATA_DIR`: directory holding the SQLite database.
- `SWRLZ_SQLITE_PATH`: exact database file path; overrides the data directory default.
- `CORS_ORIGINS`: comma-separated frontend origins, or `*` for local testing.

## Docker

```bash
docker build -t swurlz-mentor ./backend
docker run --rm -p 8000:8000 \
  -e SWRLZ_API_TOKEN='replace-me' \
  -e OPENAI_API_KEY='optional' \
  -v swurlz-data:/data \
  swurlz-mentor
```

Never place API keys in the Android APK or React source. Keep the OpenAI key on this backend only.
