import subprocess
import os
import hmac
import hashlib
from flask import Flask, request, abort

app = Flask(__name__)

DEPLOY_SCRIPT_PATH = "/home/BitBanditi/projectMariborBusi/spletnoProgramiranje/dataLoader.sh"
WEBHOOK_SECRET = os.environ.get("WEBHOOK_SECRET")

@app.route('/webhook', methods=['POST'])
def webhook():
    if request.method == 'POST':
        if WEBHOOK_SECRET:
            if 'X-Hub-Signature' not in request.headers:
                print("Error: X-Hub-Signature header missing.")
                abort(403)

            signature = request.headers['X-Hub-Signature'].split('=')[1]
            payload = request.data

            mac = hmac.new(WEBHOOK_SECRET.encode('utf-8'), payload, hashlib.sha1)
            if not hmac.compare_digest(mac.hexdigest(), signature):
                print("Error: Invalid signature.")
                abort(403)
            print("Signature verified successfully.")
        else:
            print("Warning: WEBHOOK_SECRET not set, skipping signature verification.")

        print("Executing deploy script...")
        try:
            log_path = "/var/log/webhook_deploy.log"
            os.makedirs(os.path.dirname(log_path), exist_ok=True)

            with open(log_path, "a") as log_file:
                script_dir = os.path.dirname(DEPLOY_SCRIPT_PATH)
                result = subprocess.Popen(
                    ['sudo', DEPLOY_SCRIPT_PATH],
                    stdout=log_file,
                    stderr=subprocess.STDOUT,
                    cwd=script_dir
                )
            print("Deploy script launched successfully.")
            return 'Webhook received and deploy script launched!', 200
        except Exception as e:
            print(f"Error launching deploy script: {e}")
            return f'Failed to launch deploy script: {e}', 500

    else:
        return 'Method Not Allowed', 405

if __name__ == '__main__':
    print(f"Starting webhook listener on port 8081. WEBHOOK_SECRET set: {bool(WEBHOOK_SECRET)}")
    app.run(host='0.0.0.0', port=8081)