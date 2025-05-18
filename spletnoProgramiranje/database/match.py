import json

with open('bus_stops_maribor.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

names = set()
for element in data["elements"]:
    if "tags" in element and "name" in element["tags"]:
        stop_id = element["id"]
        stop_name = element["tags"]["name"]
        names.add((stop_id, stop_name))


# print all bus stops v timijovem json filei
'''
counter = 1
print("Names of bus stops:")
for stop in names:
    print(f"{counter}: {stop}")
    counter += 1
'''

with open('bus_arrival_times_stops_maribor.json', 'r', encoding='utf-8') as f:
    arrivalData = json.load(f)

unique_names = {name for (id, name) in names}

non_matching_stops = [
    stop for stop in arrivalData 
    if stop["name"] not in unique_names
]

counter = 1
print("non matcihng stops:\n")
for stop in non_matching_stops:
    #print(f"{counter}: {stop}\n")
    counter += 1



# shrani all of the stops, ki se matchajo v obeh fileih
matching_stops = []
for stop in arrivalData:
    # print(f"{counter}: {stop}\n")
    stop_name = stop["name"]
    if any(name == stop_name for (id, name) in names):
        matching_stops.append(stop)


# print all of the stops, ki se matchajo v obeh fileih
counter = 1
for stop in matching_stops:
    print(f"{counter}: {stop}\n")
    counter += 1

output_path = 'matching_stops.json'
with open(output_path, 'w', encoding='utf-8') as out_f:
    json.dump(matching_stops, out_f, ensure_ascii=False, indent=2)


'''
search_name = "Prol. brigad - lekarna"
name_exists = any(name == search_name for (id, name) in names)
print(f"Does '{search_name}' exist? {name_exists}")
'''