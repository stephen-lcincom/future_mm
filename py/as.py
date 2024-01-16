import matplotlib.pyplot as plt
import numpy as np
import requests
import json
import mplcursors

url = "http://localhost:8084/admin_api/mm/{}/history?token={}&limit=100".format("f941dce0-3b36-11ee-856b-c32bec11e7a5", "24-edbd19dcb1ad19bcd7ed75cf6b0452c8")

payload={}
headers = {}

response = requests.request("GET", url, headers=headers, data=payload).json()

x = []
s = []
sp_a = []
sp_b = []
p_a = []
p_b = []
p_ab = []
p_bb = []
r = []
optimal_spread = []
rsvn_spread = []
inf_rsvn_spread = []
order_size = []
q = []
for item in response.get('records'):
    data = json.loads(item.get('data'))
    x.insert(0, data[0])
    s.insert(0, data[3])
    r.insert(0, data[4])
    sp_a.insert(0, data[6])
    sp_b.insert(0, data[5])
    p_a.insert(0, data[16])
    p_b.insert(0, data[17])
    rsvn_spread.insert(0, data[8]-data[7])
    inf_rsvn_spread.insert(0, data[10]-data[9])
    p_bb.insert(0, data[9])
    p_ab.insert(0, data[10])
    optimal_spread.insert(0, data[13])
    order_size.insert(0, data[14])
    q.insert(0, data[15])
# plot lines
# print(x)
# print(inf_rsvn_spread)

# fig, (ax1, ax2) = plt.subplots(ncols=2)
fig, (ax1) = plt.subplots()

ax1.plot(x, s, label = "mid price", color="black", alpha=1)
ax1.plot(x, r, label = "rsvn price", color="green", alpha=1)
ax1.plot(x, sp_a, label = "symetric bid")
ax1.plot(x, sp_b, label = "symetric ask")
ax1.plot(x, p_a, label = "inventory bid")
ax1.plot(x, p_b, label = "inventory ask")
# plt.plot(x, p_bb, label = "inf rsvn price bid")
# plt.plot(x, p_ab, label = "inf rsvn price ask")
# plt.plot(y, x, label = "line 2")

ax2 = ax1.twinx()
# ax2.set_axisbelow(True)
# ax2.grid(True, axis='y', zorder=0)
# ax2.plot(x, optimal_spread, label="optimal_spread")
# ax2.plot(x, rsvn_spread, label="rsvn_spread")
# ax2.plot(x, inf_rsvn_spread, label="inf_rsvn_spread")
# ax2.tick_params(axis='y')

# ax1.set_axisbelow(True)
# ax1.set_zorder(10)
# ax1.patch.set_visible(False)

ax1.legend(loc='upper left')
# ax2.legend()
mplcursors.cursor(hover=True)
      
fig.tight_layout()
plt.show()