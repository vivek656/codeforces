package contests.c2177;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;

public class A_OXC {

    private static class Group {
        private final int[] spinesWeights;
        int spinePerPlane;

        private Comparator<Integer> comparator;

        Group(int numSpines, int spinePerPlane, int planes) {
            spinesWeights = new int[numSpines];
            this.spinePerPlane = spinePerPlane;
            comparator = ((a, b) -> spinesWeights[a] - spinesWeights[b]);
        }

        int[] getSpinesWeights() {
            int[] ret = new int[spinesWeights.length];
            System.arraycopy(spinesWeights, 0, ret, 0, spinesWeights.length);
            return ret;
        }

        void incSpineWeightBy(int spine, int by, int plane) {
            spinesWeights[spine] = spinesWeights[spine] + by;
        }

        int getSpineWeight(int spine) {
            return spinesWeights[spine];
        }

        Integer[] spineInPlaneIterator(int plane) {
            int start = plane * spinePerPlane;
            int end = start + spinePerPlane;

            Integer[] spines = new Integer[spinePerPlane];
            for (int i = start; i < end; i++) {
                spines[i - start] = i;
            }
            Arrays.sort(spines, comparator);
            return spines;
        }
    }

    private static class Link {
        int from;
        int to;

        Link(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }

    private static class OCX {

        int[] ports;
        private int weight;

        public OCX(int numPorts) {
            ports = new int[numPorts];
            for (int i = 0; i < numPorts; i++) {
                ports[i] = -1;
            }
            weight = 0;
        }

        public void link(int start, int end) {
            if (linked(start, end)) {
                return;
            }
            int instart = ports[start];
            int inend = ports[end];

            if (instart != -1) {
                ports[instart] = -1;
                this.weight -= 2;
            }
            if (inend != -1) {
                ports[inend] = -1;
                this.weight -= 2;
            }
            ports[start] = end;
            ports[end] = start;
            this.weight += 4;
        }

        int getWeight() {
            return weight;
        }

        public boolean linked(int start, int end) {
            return ports[start] == end;
        }

        public boolean canLink(int start, int end) {
            return ports[start] == -1 && ports[end] == -1;
        }

        public boolean isLinkedToRange(int port, int toStart, int toEnd) {
            if (ports[port] == -1) return false;
            return (ports[toStart] >= toStart && ports[toEnd] > toEnd);
        }

        public int portWeight(int start, int end) {
            int res = 0;
            for (int i = start; i < end; i++) {
                if (ports[i] != -1) {
                    res += 4;
                }
            }
            return res;
        }

    }


    private static class Route {

        int port2;
        int port1;
        int ocx;

        int weight;

        Route() {
            weight = 0;
        }


    }

    static PrintWriter out = new PrintWriter(System.out);

    static class FastScanner {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StringTokenizer st = new StringTokenizer("");

        String next() {
            while (!st.hasMoreTokens())
                try {
                    st = new StringTokenizer(br.readLine());
                } catch (IOException e) {
                }
            return st.nextToken();
        }

        int nextInt() {
            return Integer.parseInt(next());
        }

        long nextLong() {
            return Long.parseLong(next());
        }
    }

    private static final FastScanner fs = new FastScanner();

    private int n, s, l, m, k, p, spinePerPlane, leafPerPlane, oxcPerPlane;
    private int numPorts, portsPerGroup, existingRouteThreshold, newRouteThreshold;
    private HashMap<Integer, Link> graph;
    private OCX[] ocxes;
    private Group[] groups;
    private Map<Integer, int[]> flowCache;

    public A_OXC(
            int n, int s, int l, int m, int k, int p
    ) {
        this.n = n;
        this.s = s;
        this.l = l;
        this.m = m;
        this.k = k;
        this.p = p;
        flowCache = new HashMap<>();
        int spinePerPlane = s / p;
        numPorts = spinePerPlane * k * n;
        portsPerGroup = s * k;
        buildGraph();
        calculateThresholds();
    }
    private void calculateThresholds() {
        // Key insight: More capacity = be pickier about reuse
        // Less capacity = reuse aggressively

        int totalSpines = s * n;  // Total spines in network
        int totalLinks = m * numPorts / 2;  // Total possible connections
        int spinesPerGroup = s;

        // Average expected flows per spine (rough estimate)
        // If we have many spines relative to groups, we can spread load
        double capacityRatio = (double)(totalSpines) / (n * n);  // Spines per group-pair

        // Existing route threshold:
        // - High capacity (many spines): be picky, prefer low load (threshold ~4-6)
        // - Low capacity (few spines): accept higher load (threshold ~12-20)
        if (s >= 8) {
            // Many spines per group - can spread load
            existingRouteThreshold = 6;
        } else if (s >= 4) {
            // Medium spines - moderate reuse
            existingRouteThreshold = 10;
        } else {
            // Few spines (s=1 or 2) - reuse aggressively
            existingRouteThreshold = 16;
        }

        // New route threshold:
        // Only create new if we find very light spines
        // With K=2, we have 2 links per spine-OXC pair
        if (k >= 2) {
            // Multiple links available - less urgent to find perfect spine
            newRouteThreshold = 4;
        } else {
            // Single link (K=1) - be pickier
            newRouteThreshold = 2;
        }

        // Adjust based on number of OXCs
        // More OXCs = more options = be pickier
        if (m >= 4) {
            existingRouteThreshold -= 2;  // Have alternatives, be picky
        }

        // Ensure minimum values
        existingRouteThreshold = Math.max(existingRouteThreshold, 4);
        newRouteThreshold = Math.max(newRouteThreshold, 2);
    }
    void buildGraph() {
        spinePerPlane = s / p;
        groups = new Group[n];
        for (int i = 0; i < n; i++) {
            groups[i] = new Group(s, spinePerPlane, p);
        }

        int numOxc = m;
        ocxes = new OCX[numOxc];
        for (int i = 0; i < numOxc; i++) {
            ocxes[i] = new A_OXC.OCX(numPorts);
        }
        oxcPerPlane = m / p;
    }

    public int[] route(int leaf1, int group1, int leaf2, int group2) {
        int key = encodeFlow(leaf1, group1, leaf2, group2);
        if (flowCache.containsKey(key)) {
            return flowCache.get(key);
        }

        Route lowestExistingRoute = null;
        for (int i = 0; i < m; i++) {
            Route route = getExistingRoute(i, group1, group2);
            if (route != null && (lowestExistingRoute == null || lowestExistingRoute.weight > route.weight)) {
                lowestExistingRoute = route;
            }

        }

        if (lowestExistingRoute != null && lowestExistingRoute.weight <= existingRouteThreshold) {
            return linkRouteAndGet(lowestExistingRoute, leaf1 , group1, leaf2, group2);
        }
        Route lowestNewRoute = null;
        for (int i = 0; i < m; i++) {
            Route route = getNewRoute(i, group1, group2);
            if (route != null && (lowestNewRoute == null || lowestNewRoute.weight > route.weight)) {
                lowestNewRoute = route;
            }
            if (lowestNewRoute != null && lowestNewRoute.weight <= newRouteThreshold) {
                break;
            }
        }
        if (lowestExistingRoute == null && lowestNewRoute == null) {
            throw new RuntimeException("No route found for " + group1 + " -> " + group2);
        }
        if(lowestExistingRoute == null) {
            return linkRouteAndGet(lowestNewRoute, leaf1, group1, leaf2, group2);
        }
        if(lowestNewRoute != null) {
            if(lowestExistingRoute.weight > lowestNewRoute.weight) linkRouteAndGet(lowestNewRoute, leaf1, group1, leaf2, group2);
        }
        return linkRouteAndGet(lowestExistingRoute, leaf1, group1, leaf2, group2);
    }

    private int[] linkRouteAndGet(Route lowestRoute, int leaf1, int group1, int leaf2, int group2) {
        int s1 = spineFromPort(lowestRoute.port1, group1, lowestRoute.ocx);
        int s2 = spineFromPort(lowestRoute.port2, group2, lowestRoute.ocx);
        int plane = lowestRoute.ocx / oxcPerPlane;
        ocxes[lowestRoute.ocx].link(lowestRoute.port1, lowestRoute.port2);
        groups[group1].incSpineWeightBy(s1, 2, plane);
        groups[group2].incSpineWeightBy(s2, 2, plane);
        int[] ret = new int[]{
                s1, linkNumberFromPortNumber(lowestRoute.port1),
                lowestRoute.ocx,
                s2, linkNumberFromPortNumber(lowestRoute.port2)
        };
        cache(ret, leaf1, group1, leaf2, group2);
        return ret;
    }

    void cache(int[] ret, int leaf1, int group1, int leaf2, int group2) {
        int key = encodeFlow(leaf1, group1, leaf2, group2);
        flowCache.put(key, ret);
        int reverseKey = encodeFlow(leaf2, group2, leaf1, group1);
        int[] retReverse = new int[]{ret[3], ret[4], ret[2], ret[0], ret[1]};
        flowCache.put(reverseKey, retReverse);
    }


    private Route getNewRoute(int ocxIndex, int g1, int g2) {
        int plane = ocxIndex / oxcPerPlane;
        OCX oxc = ocxes[ocxIndex];

        Integer[] spine1 = groups[g1].spineInPlaneIterator(plane);
        Integer[] spine2 = groups[g2].spineInPlaneIterator(plane);

        int g2PortsStart = portsPerGroup * g2 + ((spinePerPlane * k) * plane);
        int g2PortsEnd = g2PortsStart + ((spinePerPlane * k));

        int g1PortStart = portsPerGroup * g1 + ((spinePerPlane * k) * plane);
        int g1PortEnd = g1PortStart + ((spinePerPlane * k));

        int group1PlaneWeight = oxc.portWeight(g1PortStart, g1PortEnd);
        int group2PlaneWeight = oxc.portWeight(g2PortsStart, g2PortsEnd);

        Route r = null;
        for (int s1 : spine1) {
            int p1 = firstUnUsedSpinePort(oxc, g1, s1);
            if(p1 == -1) continue;
            for (int s2 : spine2 ) {
                int p2 = firstUnUsedSpinePort(oxc, g2, s2);
                if(p2 == -1) continue;
                int weight = groups[g1].spinesWeights[s1] + groups[g2].spinesWeights[s2]  + 2;
                if(r == null || r.weight > weight) {
                    r = new Route();
                    r.port1 = p1;
                    r.port2 = p2;
                    r.ocx = ocxIndex;
                    r.weight = groups[g1].spinesWeights[s1] + groups[g2].spinesWeights[s2] + group1PlaneWeight + group2PlaneWeight + 2;
                }
            }

            if(r != null && r.weight <= newRouteThreshold) {
                break;
            }
        }

        return r;
    }

    private Route getExistingRoute(int ocxIndex, int g1, int g2) {
        OCX oxc = ocxes[ocxIndex];
        int plane = ocxIndex / oxcPerPlane;

        int g2PortsStart = portsPerGroup * g2 + ((spinePerPlane * k) * plane);
        int g2PortsEnd = g2PortsStart + ((spinePerPlane * k));

        int g1PortStart = portsPerGroup * g1 + ((spinePerPlane * k) * plane);
        int g1PortEnd = g1PortStart + ((spinePerPlane * k));

        int group1PlaneWeight = oxc.portWeight(g1PortStart, g1PortEnd);
        int group2PlaneWeight = oxc.portWeight(g2PortsStart, g2PortsEnd);

        Route existingRoute = null;
        for (int s1 : groups[g1].spineInPlaneIterator(plane)) {
            if(groups[g1].getSpineWeight(s1) == 0) { continue;}
            int[] s1Port = spinePorts(s1, g1);
            for (int port = s1Port[0]; port < s1Port[1]; port++) {
                if (oxc.ports[port] != -1 && (oxc.ports[port] >= g2PortsStart && oxc.ports[port] < g2PortsEnd)) {
                    int port2 = oxc.ports[port];
                    int s2 = spineFromPort(port2, g2, ocxIndex);
                    Route r = new Route();
                    r.port1 = port;
                    r.port2 = port2;
                    r.ocx = ocxIndex;
                    r.weight = groups[g1].getSpineWeight(s1) + groups[g2].getSpineWeight(s2) + group1PlaneWeight + group2PlaneWeight;
                    if (existingRoute == null || existingRoute.weight > r.weight) {
                        existingRoute = r;
                    }
                    if (existingRoute.weight <= existingRouteThreshold) {
                        break;
                    }
                }
            }
            if (existingRoute != null && existingRoute.weight <= existingRouteThreshold) break;
        }
        return existingRoute;
    }


    private int firstUnUsedPort(OCX ocx, int group, int plane) {
        int start = portsPerGroup * group + ((spinePerPlane * k) * plane);
        int end = start + (spinePerPlane * k);
        for (int i = start; i < end; i++) {
            if (ocx.ports[i] == -1) return i;
        }
        return -1;
    }

    private int firstUnUsedSpinePort(OCX ocx, int group, int spine) {
        int[] ports = spinePorts(spine, group);
        for (int i = ports[0]; i < ports[1]; i++) {
            if (ocx.ports[i] == -1) return i;
        }
        return -1;
    }


    private int[] spinePorts(int spineNumber, int group) {
        int start = (spinePerPlane * k * group) + ((spineNumber % spinePerPlane) * k);
        int end = start + k;
        return new int[]{start, end};
    }

    private int spineFromPort(int portNumber, int group, int oxcIndex) {
        int portInGroup = portNumber - (group * portsPerGroup);
        int spineInPlane = portInGroup / k;
        int plane = oxcIndex / oxcPerPlane;
        return plane * spinePerPlane + spineInPlane;
    }

    public int[] getOCXConfig(int idx) {
        OCX ocx = ocxes[idx];
        return Arrays.copyOf(ocx.ports, ocx.ports.length);
    }

    // 1 % 1 = 0
    private int linkNumberFromPortNumber(int portNumber) {
        return portNumber % k;
    }

    private int encodeFlow(int leaf1, int group1, int leaf2, int group2) {
        return (leaf1 << 15) | (group1 << 10) | (leaf2 << 5) | group2;
    }

    public static void main(String args[]) throws IOException {
        int n = fs.nextInt();
        int s = fs.nextInt();
        int l = fs.nextInt();
        int m = fs.nextInt();
        int k = fs.nextInt();
        int p = fs.nextInt();

        // l leaves, n group
        // (l1*g1)* (l2*g2)

        for (int i = 0; i < 5; i++) {
            int q = fs.nextInt();
             int[][] routes = new int[q][5];
            A_OXC oxc = new A_OXC(n, s, l, m, k, p);
            for (int j = 0; j < q; j++) {
                int ga = fs.nextInt();
                int leaf1 = fs.nextInt();
                int gb = fs.nextInt();
                int leaf2 = fs.nextInt();
                routes[j] = oxc.route(leaf1, ga, leaf2, gb);
            }
            StringBuilder sb = new StringBuilder();
            sb.setLength(0);
            for (int o = 0; o < m; o++) {
                int[] config = oxc.getOCXConfig(o);
                for (int c : config) {
                    sb.append(c).append(" ");
                }
                out.println(sb);
                sb.setLength(0);
            }
            sb.setLength(0);
            for (int[] route : routes) {
                for (int r : route) {
                    sb.append(r).append(" ");
                }
                out.println(sb);
                sb.setLength(0);
            }
        }

        out.flush();
        out.close();

    }


}

