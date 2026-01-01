package contests.c2177;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class A_OXC {

    private static enum OCXVertexType {
        OXC, SPINE, LEAF,
    }

    private static class Link {
        int from;
        int to;

        Link(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }

    private static class Spline {

        int[][] ocxLinks;

        public Spline(int ocxes , int links) {
            ocxLinks = new int[ocxes][links];
        }

        public int checkForOcx(int ocx) {
            for (int i = 0; i < ocxLinks[0].length; i++) {
                if(ocxLinks[ocx][i] != 1) {
                    return i;
                }
            }
            return -1;
        }

        public void link(int ocx , int index) {
            ocxLinks[ocx][index] = 1;
        }
    }


    private static class OCXVertex {
        OCXVertexType type;
        private int weight = 0;

        OCXVertex(OCXVertexType type) {
            this.type = type;
        }

        int getWeight() { return  weight; }

        void incWeight(int by) {
            this.weight = weight + by;
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

    private int n, s, l, m, k, p, spinePerPlane, leafPerPlane , oxcPerPlane ;
    private double convergenceRatio;
    private int numPorts;
    private HashMap<Integer, Link> graph;
    private HashMap<OCXVertexType, OCXVertex[]> typeHashMap;
    private Spline[] splines;

    private int spineStart = 0;
    private int oxcStart;
    private int leafStart;

    public A_OXC(
            int n, int s, int l, int m, int k, int p
    ) {
        this.n = n;
        this.s = s;
        this.l = l;
        this.m = m;
        this.k = k;
        this.p = p;

        int spinePerPlane = s / p;
        int oxcPerPlane = m / p;

        int upConnectionForPine = oxcPerPlane * k;
        convergenceRatio = (0.0 + upConnectionForPine) / l;

        numPorts = spinePerPlane * k * n;
        buildGraph();
    }

    void buildGraph() {
        typeHashMap = new HashMap<>();
        int numSpines = n * s;
        splines = new Spline[m=numSpines];
        typeHashMap.put(OCXVertexType.SPINE, new OCXVertex[numSpines]);
        OCXVertex[] spines = typeHashMap.get(OCXVertexType.SPINE);
        for (int i = 0; i < numSpines; i++) {
            spines[i] = new OCXVertex(OCXVertexType.SPINE);
            splines[i] = new Spline(m , k);
        }
        spinePerPlane = s / p;
        spineStart = 0;
        //---
        leafStart = spineStart + numSpines;
        int numLeafs = n * l;
        typeHashMap.put(OCXVertexType.LEAF, new OCXVertex[numLeafs]);
        OCXVertex[] leafs = typeHashMap.get(OCXVertexType.LEAF);
        for (int i = 0; i < numLeafs; i++) {
            leafs[i] = new OCXVertex(OCXVertexType.LEAF);
        }
        leafPerPlane = l / p;
        //--
        oxcStart = leafStart + numLeafs;
        int numOxc = m;
        typeHashMap.put(OCXVertexType.OXC, new OCXVertex[numLeafs]);
        OCXVertex[] oxcs = typeHashMap.get(OCXVertexType.OXC);
        for (int i = 0; i < numOxc; i++) {
            oxcs[i] = new OCXVertex(OCXVertexType.OXC);
        }
        oxcPerPlane = m / p;
    }


    public int[] route(int leaf1, int group1 , int leaf2 , int group2) {
        int leaf1Idx  = (l * group1) + leaf1;
        int leaf2Idx  = (l * group2) + leaf2;
        int[] spinesToL1 =  adjacent(OCXVertexType.LEAF, leaf1Idx).get(OCXVertexType.SPINE);
        int[] spinesToL2 = adjacent(OCXVertexType.LEAF, leaf2Idx).get(OCXVertexType.SPINE);

        int[][] spinesWeight = new int[2][s];
        int leaf1Spine = spinesToL1[0];
        int leaf2Spine = spinesToL2[0];
        for (int i = 0; i < s; i++) {
            OCXVertex leaf1SpineData = typeHashMap.get(OCXVertexType.SPINE)[leaf1Spine++];
            OCXVertex leaf2SPineData= typeHashMap.get(OCXVertexType.SPINE)[leaf2Spine++];
            spinesWeight[0][i] = leaf1SpineData.getWeight();
            spinesWeight[1][i] = leaf2SPineData.getWeight();
        }

        int[] ocxNextLeaf1 = new int[m];
        int[] ocxNextLeaf2 = new int[m];
        int[] ocxWeight = new int[m];
        int[] ocxSplineLinkLeaf1 = new int[m];
        int[] ocxSplineLinkLeaf2 = new int[m];
        int spine1Start = spinesToL1[0];
        int spine2Start = spinesToL2[0];

        int lowestOcx = 0;
        int lowestWeight = Integer.MAX_VALUE;


        for (int ocx = 0; ocx < m; ocx++) {
            ocxWeight[ocx] = typeHashMap.get(OCXVertexType.OXC)[ocx].getWeight();
            int plane = ocx / p;
            int nextLeaf1Spine = -1;
            int leafSpine1Link = -1;
            int lowestWeightLeaf1Spine = Integer.MAX_VALUE;

            int nextLeaf2Spine = -1;
            int leafSpine2Link = -1;
            int lowestWeightLeaf2Spine = Integer.MAX_VALUE;
            for (int i = 0; i < spinePerPlane ; i++){
                int idx = (spinePerPlane*plane) + i;

                int spineLeas1Weight = spinesWeight[0][idx];
                Spline spline1 = splines[spine1Start + idx];
                int checkSpline1  = spline1.checkForOcx(ocx);
                if(checkSpline1 != -1 && spineLeas1Weight < lowestWeightLeaf1Spine){
                    lowestWeightLeaf1Spine = spineLeas1Weight;
                    nextLeaf1Spine = i;
                    leafSpine1Link = checkSpline1;
                }
                //----

                int spineLeaf2Weight = spinesWeight[1][idx];
                Spline spline2 = splines[spine2Start + idx];
                int checkSpline2  = spline2.checkForOcx(ocx);
                if(checkSpline2 != -1 && spineLeaf2Weight < lowestWeightLeaf1Spine){
                    lowestWeightLeaf2Spine = spineLeaf2Weight;
                    nextLeaf2Spine = i;
                    leafSpine2Link = checkSpline2;
                }
            }

            ocxWeight[ocx] = ocxWeight[ocx] + lowestWeightLeaf1Spine + lowestWeightLeaf2Spine;
            ocxNextLeaf1[ocx] = nextLeaf1Spine;
            ocxNextLeaf2[ocx] = nextLeaf2Spine;
            ocxSplineLinkLeaf1[ocx] = leafSpine1Link;
            ocxSplineLinkLeaf2[ocx] = leafSpine2Link;

            if(ocxWeight[ocx] < lowestWeight){
                lowestWeight = ocxWeight[ocx];
                lowestOcx = ocx;
            }
        }

        getSpine(ocxNextLeaf1[lowestOcx] , group1).incWeight(3);
        getSpine(ocxNextLeaf2[lowestOcx] , group2).incWeight(3);
        splines[spine1Start + ocxNextLeaf1[lowestOcx]].link(lowestOcx,ocxSplineLinkLeaf1[lowestOcx] );
        splines[spine2Start + ocxNextLeaf2[lowestOcx]].link(lowestOcx,ocxSplineLinkLeaf2[lowestOcx] );
        typeHashMap.get(OCXVertexType.OXC)[lowestOcx].incWeight(6);
        return new int[] {
                ocxNextLeaf1[lowestOcx],
                ocxSplineLinkLeaf1[lowestOcx],
                lowestOcx,
                ocxNextLeaf2[lowestOcx],
                ocxSplineLinkLeaf2[lowestOcx]
        };
    }

    private Map<OCXVertexType, int[]> adjacent(OCXVertexType type, int index) {
        HashMap<OCXVertexType, int[]> map = new HashMap<>();
        int[] groupAndPlane = groupAndPlane(type, index);
        switch (type) {
            case LEAF: {
                map.put(OCXVertexType.SPINE , rangeGroup(OCXVertexType.SPINE, groupAndPlane[0]));
            }
            case SPINE: {
                map.put(OCXVertexType.LEAF , rangeGroup(OCXVertexType.LEAF, groupAndPlane[0]));
                map.put(OCXVertexType.OXC , range(OCXVertexType.OXC, groupAndPlane));
            }
            case OXC: {

            }
        }
        return map;
    }

    private int[] rangeGroup(OCXVertexType type, int group) {
        return switch (type) {
            case SPINE -> new int[]{s * group, s * (group+1)};
            case LEAF -> new int[] { l * group, l * (group+1)};
            case OXC -> new int[] { 0 , m};
        };
    }

    private int[] range(OCXVertexType type, int[] groupAndPlane) {
        int group = groupAndPlane[0];
        int plane = groupAndPlane[1];
        int groupPlaneIndex = (group * p) + plane;
        return switch (type) {
            case SPINE -> new int[]{spinePerPlane * groupPlaneIndex, (spinePerPlane * (groupPlaneIndex)) + spinePerPlane};
            case LEAF -> new int[]{leafPerPlane * groupPlaneIndex, (leafPerPlane * (groupPlaneIndex)) + leafPerPlane};
            case OXC -> new int[]{oxcPerPlane * plane, (oxcPerPlane * (plane)) + oxcPerPlane};
        };
    }



    private int[] groupAndPlane(OCXVertexType type, int index) {
        return switch (type) {
            case SPINE -> new int[]{index / s, ((index % s) / p)};
            case OXC -> new int[]{-1, index / p};
            case LEAF -> new int[]{index / l, -1};
        };
    }

    private OCXVertex getSpine(int numS , int group) {
        return typeHashMap.get(OCXVertexType.SPINE)[(s * group) + numS];
    }

    public static void main(String args[]) throws IOException {
        int n = fs.nextInt();
        int s = fs.nextInt();
        int l = fs.nextInt();
        int m = fs.nextInt();
        int k = fs.nextInt();
        int p = fs.nextInt();

        int q = fs.nextInt();

        A_OXC oxc = new A_OXC(n, s, l, m, k, p);
        int[][] routes = new int[q][5];
        int[][] ocxsPostRouting = new int[q][m];
        int[][] splinesPostRouting = new int[q][s];

        for (int i = 0; i < n; i++) {
            routes[i][0] = fs.nextInt();
        }
    }



}
