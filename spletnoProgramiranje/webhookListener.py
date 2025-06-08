#!/usr/bin/env python3
import subprocess
import os
import hmac
import hashlib
from flask import Flask, request, abort

app = Flask(__name__)

DEPLOY_SCRIPT_PATH = "/home/youruser/app/deploy.sh"
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
            with open("/var/log/webhook_deploy.log", "a") as log_file:
                subprocess.Popen([DEPLOY_SCRIPT_PATH], stdout=log_file, stderr=subprocess.STDOUT)
            print("Deploy script launched successfully.")
            return 'Webhook received and deploy script launched!', 200
        except Exception as e:
            print(f"Error launching deploy script: {e}")
            return 'Failed to launch deploy script', 500
    else:
        return 'Method Not Allowed', 405

if __name__ == '__main__':
    print(f"Starting webhook listener on port 8081. WEBHOOK_SECRET set: {bool(WEBHOOK_SECRET)}")
    app.run(host='0.0.0.0', port=8081)